# Fleet Management System — Services & Controllers Reference

---
- [DESCRIPTION](Description.md)
# Routing & Dispatch Engine — Method Reference

---
# Routing & Dispatch Engine — Full Implementation Guide

> Follow these steps in order. Each step feeds directly into the next.

---

## Step 1 — Fetch Delivery Tasks from the DB

**Where:** `DispatchService.java`

The dispatch request comes in with a list of task IDs. You pull all the matching `DeliveryTaskEntity` records from the DB. Each entity already has `latitude` and `longitude` stored on it from when the delivery was created.

```java
@Service
public class DispatchService {

    @Autowired
    private DeliveryTaskRepository deliveryTaskRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private DistanceMatrixService distanceMatrixService;

    @Autowired
    private RoutingService routingService;

    @Autowired
    private RouteService routeService;

    public List<DeliveryTaskEntity> fetchDeliveryWaypoints(List<Long> taskIds) {
        List<DeliveryTaskEntity> tasks = deliveryTaskRepository.findAllById(taskIds);

        if (tasks.size() != taskIds.size()) {
            throw new ResourceNotFoundException("One or more delivery tasks not found");
        }

        for (DeliveryTaskEntity task : tasks) {
            if (task.getLatitude() == 0 || task.getLongitude() == 0) {
                throw new ValidationException("Task ID " + task.getId() + " is missing coordinates");
            }
        }

        return tasks;
    }
}
```

**What you get back:** a `List<DeliveryTaskEntity>` where each object has:
- `task.getId()`
- `task.getAddress()`
- `task.getLatitude()`
- `task.getLongitude()`
- `task.getStatus()`

---

## Step 2 — Convert Entities to Waypoints & Build the N×N Matrix

**Where:** `RoutingService.java` → `buildWaypointMatrix()` then `DistanceMatrixService.java` → `getDistanceMatrix()`

First, create the `Waypoint` helper class. This keeps `DistanceMatrixService` generic — it only knows about coordinates, not your domain entities.

```java
// src/main/java/com/fleet/dto/Waypoint.java
public class Waypoint {
    private Long taskId;
    private double lat;
    private double lng;

    public Waypoint(Long taskId, double lat, double lng) {
        this.taskId = taskId;
        this.lat = lat;
        this.lng = lng;
    }

    public Long getTaskId() { return taskId; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
}
```

Then in `RoutingService`, convert the list of entities into a list of `Waypoint` objects:

```java
// src/main/java/com/fleet/service/RoutingService.java
@Service
public class RoutingService {

    @Autowired
    private DistanceMatrixService distanceMatrixService;

    public List<Waypoint> buildWaypointMatrix(List<DeliveryTaskEntity> tasks) {
        List<Waypoint> waypoints = new ArrayList<>();

        for (DeliveryTaskEntity task : tasks) {
            waypoints.add(new Waypoint(task.getId(), task.getLatitude(), task.getLongitude()));
        }

        return waypoints;
    }
}
```

> **Note on the Depot:** Your `Description.md` says the truck always starts from a depot. For now the first task in the list acts as the anchor. If you later add a fixed depot coordinate (e.g. from `application.properties`), prepend it as `waypoints.add(0, depotWaypoint)` before passing to `getDistanceMatrix`.

Now pass the waypoint list to `DistanceMatrixService.getDistanceMatrix()` which returns the N×N matrix:

```java
// Inside RoutingService.optimizeRoute() — you will call it like this:
List<Waypoint> waypoints = buildWaypointMatrix(tasks);
double[][] matrix = distanceMatrixService.getDistanceMatrix(waypoints);
```

---

## Step 3 — Calculate Distances Using Haversine

**Where:** `DistanceMatrixService.java`

This is the full service. `getDistanceMatrix` tries the external API first, and falls back to Haversine if that fails. For now the Haversine path is the one that works — the API wiring comes after.

```java
// src/main/java/com/fleet/service/DistanceMatrixService.java
@Service
public class DistanceMatrixService {

    private static final Logger log = LoggerFactory.getLogger(DistanceMatrixService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${distance.api.google-api-key}")
    private String googleApiKey;

    @Value("${distance.api.google-base-url}")
    private String googleBaseUrl;

    // -------------------------------------------------------
    // PUBLIC ENTRY POINT — only method RoutingService calls
    // -------------------------------------------------------
    public double[][] getDistanceMatrix(List<Waypoint> waypoints) {
        try {
            return fetchFromExternalApi(waypoints);
        } catch (Exception e) {
            return handleApiFailure(e, waypoints);
        }
    }

    // -------------------------------------------------------
    // HAVERSINE — straight-line distance between two points
    // Accounts for Earth's curvature. Result is in km.
    // -------------------------------------------------------
    public double calculateDistanceBetweenPoints(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371; // Earth radius in km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1))
                 * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // distance in km
    }

    // -------------------------------------------------------
    // BUILD HAVERSINE MATRIX — loops every pair i vs j
    // -------------------------------------------------------
    private double[][] buildHaversineMatrix(List<Waypoint> waypoints) {
        int n = waypoints.size();
        double[][] matrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                } else {
                    matrix[i][j] = calculateDistanceBetweenPoints(
                        waypoints.get(i).getLat(), waypoints.get(i).getLng(),
                        waypoints.get(j).getLat(), waypoints.get(j).getLng()
                    );
                }
            }
        }

        return matrix;
    }

    // -------------------------------------------------------
    // EXTERNAL API — Google Distance Matrix
    // -------------------------------------------------------
    private double[][] fetchFromExternalApi(List<Waypoint> waypoints) {
        String payload = buildApiRequestPayload(waypoints);
        String url = googleBaseUrl + "?origins={origins}&destinations={destinations}&key={key}";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, payload, payload, googleApiKey);
        return parseApiResponse(response.getBody(), waypoints.size());
    }

    public String buildApiRequestPayload(List<Waypoint> waypoints) {
        // Google wants: lat,lng|lat,lng|lat,lng
        // OSRM wants:   lng,lat;lng,lat;lng,lat  ← opposite order, careful!
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < waypoints.size(); i++) {
            sb.append(waypoints.get(i).getLat()).append(",").append(waypoints.get(i).getLng());
            if (i < waypoints.size() - 1) sb.append("|");
        }
        return sb.toString();
    }

    public double[][] parseApiResponse(String responseBody, int n) {
        // Google returns rows[i].elements[j].distance.value (in metres)
        // Parse the JSON response and fill the N×N matrix
        double[][] matrix = new double[n][n];
        // TODO: use ObjectMapper or JsonPath to parse responseBody
        // matrix[i][j] = rows.get(i).elements.get(j).distance.value / 1000.0;
        return matrix;
    }

    // -------------------------------------------------------
    // FALLBACK — called when external API throws any exception
    // Never throws — always returns a valid matrix
    // -------------------------------------------------------
    public double[][] handleApiFailure(Exception e, List<Waypoint> waypoints) {
        log.warn("External distance API failed. Falling back to Haversine. Reason: {}", e.getMessage());
        return buildHaversineMatrix(waypoints);
    }
}
```

**What you get back:** a `double[][] matrix` where `matrix[i][j]` = distance in km from waypoint `i` to waypoint `j`, and `matrix[i][i] = 0`.

---

## Step 4 — Apply Greedy TSP

**Where:** `RoutingService.java`

You now have the matrix. The TSP picks the nearest unvisited stop at every step and builds the optimal sequence. The full `RoutingService` looks like this:

```java
// src/main/java/com/fleet/service/RoutingService.java
@Service
public class RoutingService {

    @Autowired
    private DistanceMatrixService distanceMatrixService;

    // -------------------------------------------------------
    // ENTRY POINT — called by DispatchService
    // -------------------------------------------------------
    public List<DeliveryTaskEntity> optimizeRoute(List<DeliveryTaskEntity> tasks) {
        // Step A: convert entities to waypoints
        List<Waypoint> waypoints = buildWaypointMatrix(tasks);

        // Step B: get N×N distance matrix (Haversine or external API)
        double[][] matrix = distanceMatrixService.getDistanceMatrix(waypoints);

        // Step C: run greedy TSP — returns ordered index sequence
        List<Integer> sequence = applyGreedyTSP(matrix);

        // Step D: map index sequence back to actual task entities
        return sequenceWaypoints(sequence, tasks);
    }

    // -------------------------------------------------------
    // CONVERT entities → Waypoint list (preserves index order)
    // -------------------------------------------------------
    public List<Waypoint> buildWaypointMatrix(List<DeliveryTaskEntity> tasks) {
        List<Waypoint> waypoints = new ArrayList<>();
        for (DeliveryTaskEntity task : tasks) {
            waypoints.add(new Waypoint(task.getId(), task.getLatitude(), task.getLongitude()));
        }
        return waypoints;
    }

    // -------------------------------------------------------
    // GREEDY TSP — nearest neighbour heuristic
    // Always starts from index 0 (first task / depot)
    // Returns ordered list of indices, e.g. [0, 3, 1, 4, 2]
    // -------------------------------------------------------
    public List<Integer> applyGreedyTSP(double[][] distanceMatrix) {
        int n = distanceMatrix.length;
        boolean[] visited = new boolean[n];
        List<Integer> route = new ArrayList<>();

        int current = 0; // start at index 0 (depot or first stop)
        visited[current] = true;
        route.add(current);

        for (int step = 1; step < n; step++) {
            int nearest = -1;
            double minDist = Double.MAX_VALUE;

            for (int j = 0; j < n; j++) {
                if (!visited[j] && distanceMatrix[current][j] < minDist) {
                    minDist = distanceMatrix[current][j];
                    nearest = j;
                }
            }

            visited[nearest] = true;
            route.add(nearest);
            current = nearest;
        }

        return route;
    }

    // -------------------------------------------------------
    // MAP index sequence → actual DeliveryTaskEntity objects
    // -------------------------------------------------------
    public List<DeliveryTaskEntity> sequenceWaypoints(List<Integer> sequence, List<DeliveryTaskEntity> tasks) {
        List<DeliveryTaskEntity> ordered = new ArrayList<>();
        for (int index : sequence) {
            ordered.add(tasks.get(index));
        }
        return ordered;
    }

    // -------------------------------------------------------
    // SUM total distance for the final ordered sequence
    // -------------------------------------------------------
    public double calculateTotalDistance(List<Integer> sequence, double[][] distanceMatrix) {
        double total = 0;
        for (int i = 0; i < sequence.size() - 1; i++) {
            total += distanceMatrix[sequence.get(i)][sequence.get(i + 1)];
        }
        return total;
    }
}
```

**What you get back:** `List<DeliveryTaskEntity>` in the optimized delivery order, e.g. [Task C, Task A, Task B] instead of [Task A, Task B, Task C].

---

## Step 5 — Expose via DispatchController

### 5a — DispatchRequestDTO (input)

```java
// src/main/java/com/fleet/dto/DispatchRequestDTO.java
public class DispatchRequestDTO {
    private Long vehicleId;
    private Long driverId;
    private List<Long> deliveryTaskIds;

    // getters and setters
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public List<Long> getDeliveryTaskIds() { return deliveryTaskIds; }
    public void setDeliveryTaskIds(List<Long> deliveryTaskIds) { this.deliveryTaskIds = deliveryTaskIds; }
}
```

### 5b — OptimizedRouteResponseDTO (output)

```java
// src/main/java/com/fleet/dto/OptimizedRouteResponseDTO.java
public class OptimizedRouteResponseDTO {
    private Long routeId;
    private Long vehicleId;
    private Long driverId;
    private List<OrderedDeliveryStop> stops;
    private double totalDistanceKm;
    private String status;

    // getters and setters
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public List<OrderedDeliveryStop> getStops() { return stops; }
    public void setStops(List<OrderedDeliveryStop> stops) { this.stops = stops; }

    public double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // -------------------------------------------------------
    // INNER CLASS — each individual stop in the ordered route
    // -------------------------------------------------------
    public static class OrderedDeliveryStop {
        private int sequence;
        private Long taskId;
        private String address;
        private double latitude;
        private double longitude;
        private double distanceFromPreviousKm;

        // getters and setters
        public int getSequence() { return sequence; }
        public void setSequence(int sequence) { this.sequence = sequence; }

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public double getDistanceFromPreviousKm() { return distanceFromPreviousKm; }
        public void setDistanceFromPreviousKm(double d) { this.distanceFromPreviousKm = d; }
    }
}
```

### 5c — Full DispatchService (orchestrator — ties all steps together)

```java
// src/main/java/com/fleet/service/DispatchService.java
@Service
public class DispatchService {

    @Autowired
    private DeliveryTaskRepository deliveryTaskRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private DistanceMatrixService distanceMatrixService;

    @Autowired
    private RoutingService routingService;

    @Autowired
    private RouteService routeService;

    // -------------------------------------------------------
    // MAIN METHOD — full pipeline, called by controller
    // -------------------------------------------------------
    @Transactional
    public OptimizedRouteResponseDTO dispatchRoute(DispatchRequestDTO request) {

        // Step 1: validate
        validateDispatchRequest(request);

        // Step 2: fetch tasks from DB
        List<DeliveryTaskEntity> tasks = fetchDeliveryWaypoints(request.getDeliveryTaskIds());

        // Step 3 + 4: build matrix and run TSP (both inside RoutingService)
        List<DeliveryTaskEntity> orderedTasks = routingService.optimizeRoute(tasks);

        // Step 5: calculate total distance for the final sequence
        List<Waypoint> waypoints = routingService.buildWaypointMatrix(tasks);
        double[][] matrix = distanceMatrixService.getDistanceMatrix(waypoints);
        List<Integer> sequence = routingService.applyGreedyTSP(matrix);
        double totalDistance = routingService.calculateTotalDistance(sequence, matrix);

        // Step 6: mark vehicle as IN_USE, driver as ON_SHIFT
        assignVehicleAndDriver(request.getVehicleId(), request.getDriverId());

        // Step 7: save a Route record
        RouteEntity savedRoute = routeService.createRoute(request.getVehicleId(), request.getDriverId());

        // Step 8: build and return response DTO
        return buildOptimizedRouteResponse(savedRoute.getId(), request.getVehicleId(),
                request.getDriverId(), orderedTasks, totalDistance, matrix, sequence);
    }

    // -------------------------------------------------------
    // STEP 1 — validate vehicle + driver exist and are free
    // -------------------------------------------------------
    public void validateDispatchRequest(DispatchRequestDTO request) {
        VehicleEntity vehicle = vehicleRepository.findById(request.getVehicleId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.getVehicleId()));

        DriverEntity driver = driverRepository.findById(request.getDriverId())
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + request.getDriverId()));

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ValidationException("Vehicle is not available. Current status: " + vehicle.getStatus());
        }

        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new ValidationException("Driver is not available. Current status: " + driver.getStatus());
        }

        if (driver.getLicenseValidUntil().isBefore(LocalDate.now())) {
            throw new ValidationException("Driver license has expired: " + driver.getLicenseValidUntil());
        }

        int taskCount = request.getDeliveryTaskIds().size();
        if (taskCount < 1 || taskCount > 10) {
            throw new ValidationException("Task count must be between 1 and 10. Got: " + taskCount);
        }
    }

    // -------------------------------------------------------
    // STEP 2 — pull tasks from DB, check coordinates exist
    // -------------------------------------------------------
    public List<DeliveryTaskEntity> fetchDeliveryWaypoints(List<Long> taskIds) {
        List<DeliveryTaskEntity> tasks = deliveryTaskRepository.findAllById(taskIds);

        if (tasks.size() != taskIds.size()) {
            throw new ResourceNotFoundException("One or more delivery tasks not found");
        }

        for (DeliveryTaskEntity task : tasks) {
            if (task.getLatitude() == 0 || task.getLongitude() == 0) {
                throw new ValidationException("Task ID " + task.getId() + " is missing coordinates");
            }
        }

        return tasks;
    }

    // -------------------------------------------------------
    // STEP 6 — mark vehicle IN_USE, driver ON_SHIFT
    // -------------------------------------------------------
    public void assignVehicleAndDriver(Long vehicleId, Long driverId) {
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        vehicle.setStatus(VehicleStatus.IN_USE);
        vehicleRepository.save(vehicle);

        DriverEntity driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        driver.setStatus(DriverStatus.ON_SHIFT);
        driverRepository.save(driver);
    }

    // -------------------------------------------------------
    // STEP 8 — assemble the final response DTO
    // -------------------------------------------------------
    public OptimizedRouteResponseDTO buildOptimizedRouteResponse(
            Long routeId, Long vehicleId, Long driverId,
            List<DeliveryTaskEntity> orderedTasks,
            double totalDistance,
            double[][] matrix,
            List<Integer> sequence) {

        OptimizedRouteResponseDTO response = new OptimizedRouteResponseDTO();
        response.setRouteId(routeId);
        response.setVehicleId(vehicleId);
        response.setDriverId(driverId);
        response.setTotalDistanceKm(Math.round(totalDistance * 100.0) / 100.0);
        response.setStatus(RouteStatus.PLANNED.name());

        List<OptimizedRouteResponseDTO.OrderedDeliveryStop> stops = new ArrayList<>();

        for (int i = 0; i < orderedTasks.size(); i++) {
            DeliveryTaskEntity task = orderedTasks.get(i);
            OptimizedRouteResponseDTO.OrderedDeliveryStop stop =
                new OptimizedRouteResponseDTO.OrderedDeliveryStop();

            stop.setSequence(i + 1);
            stop.setTaskId(task.getId());
            stop.setAddress(task.getAddress());
            stop.setLatitude(task.getLatitude());
            stop.setLongitude(task.getLongitude());

            // distance from the previous stop (0 for the first stop)
            if (i == 0) {
                stop.setDistanceFromPreviousKm(0);
            } else {
                stop.setDistanceFromPreviousKm(
                    Math.round(matrix[sequence.get(i - 1)][sequence.get(i)] * 100.0) / 100.0
                );
            }

            stops.add(stop);
        }

        response.setStops(stops);
        return response;
    }
}
```

### 5d — DispatchController

```java
// src/main/java/com/fleet/controller/DispatchController.java
@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private RouteService routeService;

    // POST /api/dispatch/optimize
    @PostMapping("/optimize")
    public ResponseEntity<OptimizedRouteResponseDTO> dispatchRoute(
            @RequestBody DispatchRequestDTO request) {
        OptimizedRouteResponseDTO response = dispatchService.dispatchRoute(request);
        return ResponseEntity.ok(response);
    }

    // GET /api/dispatch/status/{routeId}
    @GetMapping("/status/{routeId}")
    public ResponseEntity<RouteEntity> getDispatchStatus(@PathVariable Long routeId) {
        RouteEntity route = routeService.getRouteById(routeId);
        return ResponseEntity.ok(route);
    }

    // POST /api/dispatch/validate
    @PostMapping("/validate")
    public ResponseEntity<String> validateDispatchRequest(
            @RequestBody DispatchRequestDTO request) {
        dispatchService.validateDispatchRequest(request);
        return ResponseEntity.ok("Dispatch request is valid");
    }
}
```

---

## End-to-End Flow Summary

```
POST /api/dispatch/optimize
{
  "vehicleId": 1,
  "driverId": 2,
  "deliveryTaskIds": [10, 11, 12, 13, 14]
}

  ↓ DispatchController.dispatchRoute()
  ↓ DispatchService.dispatchRoute()

[1] validateDispatchRequest()      → vehicle + driver exist, available, license valid
[2] fetchDeliveryWaypoints()       → pull DeliveryTaskEntity records from DB
[3] buildWaypointMatrix()          → DeliveryTaskEntity list → List<Waypoint>
[4] getDistanceMatrix()            → List<Waypoint> → double[][] N×N matrix (Haversine or API)
[5] applyGreedyTSP()               → double[][] → List<Integer> optimized index sequence
[6] sequenceWaypoints()            → List<Integer> → ordered List<DeliveryTaskEntity>
[7] calculateTotalDistance()       → sum of edges along the sequence → double km
[8] assignVehicleAndDriver()       → vehicle → IN_USE, driver → ON_SHIFT
[9] routeService.createRoute()     → save RouteEntity with status PLANNED
[10] buildOptimizedRouteResponse() → assemble OptimizedRouteResponseDTO

  ↓ Response

{
  "routeId": 99,
  "vehicleId": 1,
  "driverId": 2,
  "totalDistanceKm": 24.7,
  "status": "PLANNED",
  "stops": [
    { "sequence": 1, "taskId": 13, "address": "...", "latitude": 13.05, "longitude": 80.24, "distanceFromPreviousKm": 0 },
    { "sequence": 2, "taskId": 10, "address": "...", "latitude": 13.06, "longitude": 80.25, "distanceFromPreviousKm": 3.2 },
    { "sequence": 3, "taskId": 12, "address": "...", "latitude": 13.07, "longitude": 80.22, "distanceFromPreviousKm": 5.1 },
    ...
  ]
}
```

---

## Files Created / Modified in This Guide

| File | Status |
|------|--------|
| `dto/Waypoint.java` | New |
| `dto/DispatchRequestDTO.java` | New |
| `dto/OptimizedRouteResponseDTO.java` | New |
| `service/DistanceMatrixService.java` | New |
| `service/RoutingService.java` | New |
| `service/DispatchService.java` | New |
| `controller/DispatchController.java` | New |

---


## DistanceMatrixService *(External API)*

| Method | Responsibility |
|--------|----------------|
| `getDistanceMatrix(waypoints)` | Public entry — tries external API, falls back to Haversine on failure |
| `calculateDistanceBetweenPoints(lat1, lng1, lat2, lng2)` | Haversine formula — pure math, no network call |
| `buildApiRequestPayload(waypoints)` | Formats coordinates into the request shape required by Google/OSRM |
| `parseApiResponse(responseBody)` | Extracts the distance matrix (2D array) from the raw API response |
| `handleApiFailure(exception, waypoints)` | Catches API/network errors, logs, and triggers the Haversine fallback path |

**Notes for implementation:**
- `getDistanceMatrix` is the only method `RoutingService` should call directly — it should never know whether the data came from Google, OSRM, or Haversine.
- Watch coordinate order: OSRM wants `longitude,latitude`; Google wants `latitude,longitude`. Document this clearly in `buildApiRequestPayload`.
- `handleApiFailure` should not throw — it should return a Haversine-built matrix so dispatch never hard-fails just because the external API is down.

---

## RoutingService *(TSP Optimization)*

| Method | Responsibility |
|--------|----------------|
| `optimizeRoute(waypoints, distanceMatrix)` | Entry point — orchestrates matrix build → greedy TSP → returns sequenced stops |
| `applyGreedyTSP(distanceMatrix, startIndex)` | Nearest-neighbor heuristic: from current stop, repeatedly pick closest unvisited stop |
| `calculateTotalDistance(sequence, distanceMatrix)` | Sums edge weights along a given stop order |
| `buildWaypointMatrix(deliveryTasks)` | Converts a list of `DeliveryTaskEntity` into an ordered list of (lat, lng) pairs, depot first |
| `sequenceWaypoints(route, originalTasks)` | Maps the optimized index order back to actual `DeliveryTaskEntity` objects for the response |

**Notes for implementation:**
- `applyGreedyTSP` always needs an explicit starting index — usually the depot (index 0). Don't let the algorithm pick its own start.
- `calculateTotalDistance` should be reused both inside the optimizer (to validate improvement, if you add 2-opt later) and to populate `OptimizedRouteResponseDTO.totalDistance`.
- Keep `buildWaypointMatrix` and `sequenceWaypoints` symmetric — same index ordering in both directions, or your final manifest will silently point to the wrong addresses.

---
## DispatchService *(Orchestrator)*

| Method | Responsibility |
|--------|----------------|
| `dispatchRoute(dispatchRequestDTO)` | Full pipeline: validate → assign → fetch waypoints → optimize → build response |
| `validateDispatchRequest(dto)` | Checks vehicle/driver exist, are available, and task count is within bounds (5–10) |
| `assignVehicleAndDriver(vehicleId, driverId, taskIds)` | Links tasks to the chosen vehicle/driver before optimization |
| `fetchDeliveryWaypoints(taskIds)` | Pulls `DeliveryTaskEntity` records and converts to coordinate list |
| `buildOptimizedRouteResponse(sequence, totalDistance)` | Assembles the final `OptimizedRouteResponseDTO` for the controller |

**Notes for implementation:**
- `validateDispatchRequest` is step 1, always — never let an invalid vehicle/driver reach the routing logic mid-flow.
- `dispatchRoute` should be `@Transactional` since it writes route/task assignments before returning.
- Order inside `dispatchRoute`: validate → assign → fetch waypoints → `RoutingService.optimizeRoute` → build response. Keep `DispatchService` as pure orchestration — no distance math or TSP logic should live here.

---

## DispatchController — `/api/dispatch`

| Method | Endpoint | Handler | Calls |
|--------|----------|---------|-------|
| POST | `/optimize` | `dispatchRoute` | `DispatchService.dispatchRoute` |
| GET | `/status/{routeId}` | `getDispatchStatus` | `RouteService.getRouteById` |
| POST | `/validate` | `validateDispatchRequest` | `DispatchService.validateDispatchRequest` |

---

## Suggested Build Order

1. `DistanceMatrixService.calculateDistanceBetweenPoints` (Haversine) — no dependencies, easiest to unit test
2. `DistanceMatrixService.getDistanceMatrix` (Haversine-only first, wire in API later)
3. `RoutingService.applyGreedyTSP` + `calculateTotalDistance` — test against a hardcoded matrix
4. `RoutingService.buildWaypointMatrix` / `sequenceWaypoints` — connects entities to the algorithm
5. `RoutingService.optimizeRoute` — ties 1–4 together
6. `DispatchService` methods, in the table order above
7. Wire in the real external API (`buildApiRequestPayload`, `parseApiResponse`, `handleApiFailure`) last, once the Haversine path works end-to-end

This order lets you validate the math and TSP logic in isolation before touching network calls or persistence.