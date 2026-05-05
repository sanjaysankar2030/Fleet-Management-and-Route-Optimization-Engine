# Fleet Management System — Complete Project Reference

---

## Table of Contents
1. [What This Project Is](#1-what-this-project-is)
2. [Tech Stack](#2-tech-stack)
3. [Project Structure](#3-project-structure)
4. [The Two Big Parts](#4-the-two-big-parts)
5. [Database Schema](#5-database-schema)
6. [Entities & Relationships](#6-entities--relationships)
7. [Enums](#7-enums)
8. [Repository Layer](#8-repository-layer)
9. [Service Layer — All Methods](#9-service-layer--all-methods)
10. [Controller Layer — All Endpoints](#10-controller-layer--all-endpoints)
11. [DTOs](#11-dtos)
12. [The Routing Engine — How It Works](#12-the-routing-engine--how-it-works)
13. [Distance Matrix — The Math](#13-distance-matrix--the-math)
14. [Greedy TSP Algorithm](#14-greedy-tsp-algorithm)
15. [External API Integration](#15-external-api-integration)
16. [Full Dispatch Flow — End to End](#16-full-dispatch-flow--end-to-end)
17. [Validation Rules](#17-validation-rules)
18. [Exception Handling](#18-exception-handling)
19. [application.properties Configuration](#19-applicationproperties-configuration)
20. [Week 1 vs Week 2 Scope](#20-week-1-vs-week-2-scope)
21. [Common Mistakes to Avoid](#21-common-mistakes-to-avoid)

---

## 1. What This Project Is

A **Fleet Management Backend** built with Spring Boot. It helps a logistics company manage their trucks, drivers, and deliveries efficiently — and most importantly, automatically calculates the **most efficient delivery route** for each truck.

Think of it as a simplified internal system used by courier companies like FedEx, Swiggy, or Dunzo.

**Core Goals:**
- Maintain a clean registry of vehicles, drivers, delivery tasks, and routes
- Given a set of delivery addresses, calculate the shortest possible route
- Integrate with an external mapping service for real road distances
- Return an optimized, sequenced delivery order to the dispatcher

---

## 2. Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17+ |
| Framework | Spring Boot |
| Database | MySQL |
| ORM | Spring Data JPA / Hibernate |
| HTTP Client | RestTemplate or WebClient |
| External API | Google Distance Matrix API or OpenStreetMap (OSRM) |
| Build Tool | Maven or Gradle |
| Utilities | Lombok |

---

## 3. Project Structure

```
src/main/java/com/fleet/
│
├── entity/
│   ├── DriverEntity.java          ✅ done
│   ├── VehicleEntity.java
│   ├── DeliveryTaskEntity.java
│   ├── RouteEntity.java
│   └── enums/
│       ├── VehicleStatus.java
│       ├── DeliveryStatus.java
│       ├── RouteStatus.java
│       └── DriverStatus.java
│
├── repository/
│   ├── DriverRepository.java      ✅ done
│   ├── VehicleRepository.java
│   ├── DeliveryTaskRepository.java
│   └── RouteRepository.java
│
├── service/
│   ├── DriverService.java
│   ├── VehicleService.java
│   ├── DeliveryTaskService.java
│   ├── RouteService.java
│   ├── RoutingService.java
│   ├── DistanceMatrixService.java
│   └── DispatchService.java
│
├── controller/
│   ├── DriverController.java
│   ├── VehicleController.java
│   ├── DeliveryTaskController.java
│   ├── RouteController.java
│   └── DispatchController.java
│
├── dto/
│   ├── VehicleRequestDTO.java
│   ├── VehicleResponseDTO.java
│   ├── DriverRequestDTO.java
│   ├── DriverResponseDTO.java
│   ├── DeliveryTaskRequestDTO.java
│   ├── DeliveryTaskResponseDTO.java
│   ├── RouteRequestDTO.java
│   ├── RouteResponseDTO.java
│   ├── DispatchRequestDTO.java
│   └── OptimizedRouteResponseDTO.java
│
├── config/
│   ├── DistanceMatrixConfig.java
│   └── RestTemplateConfig.java
│
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    └── ValidationException.java
```

---

## 4. The Two Big Parts

### Part 1 — Fleet Registry (CRUD / Data Layer)
Structured database management for:
- **Vehicles** — capacity, license plate, maintenance status
- **Drivers** — license validity, shift hours
- **Delivery Tasks** — addresses with lat/lng coordinates
- **Routes** — a planned journey for a truck on a given day

No smart logic here. Just keeping records clean, valid, and queryable.

### Part 2 — Dispatch & Routing Engine (Smart Layer)
Given 5–10 delivery addresses for a truck:
1. Fetch coordinates for each delivery point
2. Build a distance matrix (every point vs every point)
3. Run Greedy TSP to find the shortest route
4. Return the addresses in the optimized delivery sequence

---

## 5. Database Schema

```sql
-- Drivers
CREATE TABLE driver (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    license_number VARCHAR(100) UNIQUE NOT NULL,
    license_valid_until DATE,
    shift_hours DOUBLE,
    status VARCHAR(50)
);

-- Vehicles
CREATE TABLE vehicle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_plate VARCHAR(100) UNIQUE NOT NULL,
    capacity DOUBLE NOT NULL,
    status VARCHAR(50),
    last_maintenance_date DATE,
    driver_id BIGINT,
    FOREIGN KEY (driver_id) REFERENCES driver(id)
);

-- Delivery Tasks
CREATE TABLE delivery_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    address VARCHAR(500) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    status VARCHAR(50),
    vehicle_id BIGINT,
    driver_id BIGINT,
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (driver_id) REFERENCES driver(id)
);

-- Routes
CREATE TABLE route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    status VARCHAR(50),
    vehicle_id BIGINT,
    driver_id BIGINT,
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (driver_id) REFERENCES driver(id)
);
```

> Note: With `spring.jpa.hibernate.ddl-auto=update`, Hibernate auto-generates these tables. You don't need to run this SQL manually — it's here for understanding the schema.

---

## 6. Entities & Relationships

### DriverEntity ✅
```
id | name | licenseNumber | licenseValidUntil | shiftHours | status
```

### VehicleEntity
```
id | licensePlate | capacity | status (ENUM) | lastMaintenanceDate
ManyToOne → DriverEntity
```

### DeliveryTaskEntity
```
id | address | latitude | longitude | status (ENUM)
ManyToOne → VehicleEntity
ManyToOne → DriverEntity
```

### RouteEntity
```
id | date | status (ENUM)
ManyToOne → VehicleEntity
ManyToOne → DriverEntity
```

**Relationship Summary:**
- One Driver → Many DeliveryTasks
- One Vehicle → Many DeliveryTasks
- One Vehicle → One Driver (assigned)
- One Route → One Vehicle + One Driver

---

## 7. Enums

### VehicleStatus
```java
AVAILABLE, IN_USE, MAINTENANCE
```

### DeliveryStatus
```java
UNASSIGNED, ASSIGNED, DELIVERED
```

### RouteStatus
```java
PLANNED, ACTIVE, COMPLETED
```

### DriverStatus
```java
AVAILABLE, ON_SHIFT, OFF_SHIFT
```

> All enums go in package: `entity.enums`
> Always annotate with `@Enumerated(EnumType.STRING)` in entities — stores readable strings in DB instead of ordinal numbers.

---

## 8. Repository Layer

All repositories extend `JpaRepository<Entity, Long>` which gives you:
- `save()`, `findById()`, `findAll()`, `deleteById()` — for free

```java
public interface VehicleRepository extends JpaRepository<VehicleEntity, Long> {
    List<VehicleEntity> findByStatus(VehicleStatus status); // custom query
}

public interface DeliveryTaskRepository extends JpaRepository<DeliveryTaskEntity, Long> {
    List<DeliveryTaskEntity> findByVehicleId(Long vehicleId);
    List<DeliveryTaskEntity> findByDriverId(Long driverId);
}

public interface RouteRepository extends JpaRepository<RouteEntity, Long> {
    List<RouteEntity> findByVehicleId(Long vehicleId);
    List<RouteEntity> findByStatus(RouteStatus status);
}
```

---

## 9. Service Layer — All Methods

### DriverService
| Method | Responsibility |
|--------|---------------|
| `registerDriver` | Save new driver, validate license not empty |
| `getDriverById` | Fetch or throw ResourceNotFoundException |
| `getAllDrivers` | Return all drivers |
| `updateDriverShiftHours` | Update shift hours, validate > 0 |
| `validateDriverLicense` | Check license expiry date |
| `isDriverAvailable` | Check status == AVAILABLE |

### VehicleService
| Method | Responsibility |
|--------|---------------|
| `registerVehicle` | Save vehicle, validate capacity > 0, plate not empty |
| `getVehicleById` | Fetch or throw ResourceNotFoundException |
| `getAllVehicles` | Return all vehicles |
| `getAvailableVehicles` | Filter by status == AVAILABLE |
| `assignDriverToVehicle` | Link driver to vehicle, validate driver exists |
| `updateVehicleStatus` | Change status enum value |
| `updateMaintenanceDate` | Set lastMaintenanceDate, set status MAINTENANCE |
| `validateVehicleCapacity` | Throw if capacity <= 0 |

### DeliveryTaskService
| Method | Responsibility |
|--------|---------------|
| `createDeliveryTask` | Save task, validate coordinates not null |
| `getDeliveryTaskById` | Fetch or throw |
| `getAllDeliveryTasks` | Return all tasks |
| `assignTaskToVehicle` | Link vehicle, update status to ASSIGNED |
| `assignTaskToDriver` | Link driver to task |
| `updateDeliveryStatus` | Change delivery status |
| `getTasksByVehicle` | Filter by vehicleId |
| `getTasksByDriver` | Filter by driverId |
| `validateCoordinates` | Throw if lat/lng null or out of range |

### RouteService
| Method | Responsibility |
|--------|---------------|
| `createRoute` | Save route with PLANNED status |
| `getRouteById` | Fetch or throw |
| `getAllRoutes` | Return all routes |
| `getRoutesByVehicle` | Filter by vehicleId |
| `getRoutesByDriver` | Filter by driverId |
| `updateRouteStatus` | Change route status (PLANNED→ACTIVE→COMPLETED) |
| `getActiveRoutes` | Filter by status == ACTIVE |

### RoutingService *(TSP Core)*
| Method | Responsibility |
|--------|---------------|
| `optimizeRoute` | Entry point — takes list of tasks, returns ordered list |
| `applyGreedyTSP` | Greedy nearest-neighbour algorithm |
| `calculateTotalDistance` | Sum all distances in the final sequence |
| `buildWaypointMatrix` | Build N×N distance matrix from coordinates |
| `sequenceWaypoints` | Convert matrix result into ordered task list |

### DistanceMatrixService *(External API)*
| Method | Responsibility |
|--------|---------------|
| `getDistanceMatrix` | Call Google/OSRM API with all coordinate pairs |
| `calculateDistanceBetweenPoints` | Haversine fallback for two points |
| `buildApiRequestPayload` | Format coordinates into API request body |
| `parseApiResponse` | Extract distances from API JSON response |
| `handleApiFailure` | On API error, fall back to Haversine |

### DispatchService *(Orchestrator)*
| Method | Responsibility |
|--------|---------------|
| `dispatchRoute` | Main method — full end-to-end flow |
| `validateDispatchRequest` | Check vehicle + driver exist and are available |
| `assignVehicleAndDriver` | Set vehicle status IN_USE, driver ON_SHIFT |
| `fetchDeliveryWaypoints` | Get all tasks assigned to the vehicle |
| `buildOptimizedRouteResponse` | Package final ordered route into response DTO |

---

## 10. Controller Layer — All Endpoints

### DriverController — `/api/drivers`
| Method | Endpoint | Service Call |
|--------|----------|-------------|
| POST | `/` | `registerDriver` |
| GET | `/` | `getAllDrivers` |
| GET | `/{id}` | `getDriverById` |
| PUT | `/{id}/shift-hours` | `updateDriverShiftHours` |
| GET | `/{id}/availability` | `isDriverAvailable` |

### VehicleController — `/api/vehicles`
| Method | Endpoint | Service Call |
|--------|----------|-------------|
| POST | `/` | `registerVehicle` |
| GET | `/` | `getAllVehicles` |
| GET | `/{id}` | `getVehicleById` |
| GET | `/available` | `getAvailableVehicles` |
| PUT | `/{id}/status` | `updateVehicleStatus` |
| PUT | `/{id}/assign-driver` | `assignDriverToVehicle` |
| PUT | `/{id}/maintenance` | `updateMaintenanceDate` |

### DeliveryTaskController — `/api/deliveries`
| Method | Endpoint | Service Call |
|--------|----------|-------------|
| POST | `/` | `createDeliveryTask` |
| GET | `/` | `getAllDeliveryTasks` |
| GET | `/{id}` | `getDeliveryTaskById` |
| PUT | `/{id}/assign-vehicle` | `assignTaskToVehicle` |
| PUT | `/{id}/assign-driver` | `assignTaskToDriver` |
| PUT | `/{id}/status` | `updateDeliveryStatus` |
| GET | `/vehicle/{vehicleId}` | `getTasksByVehicle` |
| GET | `/driver/{driverId}` | `getTasksByDriver` |

### RouteController — `/api/routes`
| Method | Endpoint | Service Call |
|--------|----------|-------------|
| POST | `/` | `createRoute` |
| GET | `/` | `getAllRoutes` |
| GET | `/{id}` | `getRouteById` |
| GET | `/vehicle/{vehicleId}` | `getRoutesByVehicle` |
| GET | `/driver/{driverId}` | `getRoutesByDriver` |
| PUT | `/{id}/status` | `updateRouteStatus` |
| GET | `/active` | `getActiveRoutes` |

### DispatchController — `/api/dispatch`
| Method | Endpoint | Service Call |
|--------|----------|-------------|
| POST | `/optimize` | `dispatchRoute` |
| GET | `/status/{routeId}` | `getDispatchStatus` |
| POST | `/validate` | `validateDispatchRequest` |

---

## 11. DTOs

### DispatchRequestDTO *(input to the optimizer)*
```java
Long vehicleId;
Long driverId;
List<Long> deliveryTaskIds;  // IDs of tasks to optimize
```

### OptimizedRouteResponseDTO *(output of the optimizer)*
```java
Long routeId;
Long vehicleId;
Long driverId;
List<OrderedDeliveryStop> stops;  // ordered sequence
double totalDistanceKm;
String status;

// Inner class
class OrderedDeliveryStop {
    int sequence;          // 1, 2, 3...
    Long taskId;
    String address;
    double latitude;
    double longitude;
    double distanceFromPreviousKm;
}
```

---

## 12. The Routing Engine — How It Works

### The Problem
Given N delivery points, find the shortest path that visits all of them exactly once. This is the **Travelling Salesman Problem (TSP)**.

### Why Greedy (Nearest Neighbour)?
- True optimal TSP is NP-hard — too slow for real-time use
- Greedy gives a "good enough" solution in O(N²) time
- Standard approach for MVP logistics systems

### The Key Concept: Depot
The truck always starts from a **depot** (warehouse/origin point). This is your starting coordinate. You must always include this in your request or hardcode it for now.

```
Depot → Stop 1 → Stop 2 → Stop 3 → ... → Stop N
```

---

## 13. Distance Matrix — The Math

### What It Is
An N×N grid where each cell `[i][j]` = distance from point i to point j.

```
         Depot   A      B      C
Depot      0    5km    8km    3km
A         5km    0     4km    7km
B         8km   4km    0      2km
C         3km   7km   2km     0
```

### Option A — Haversine Formula (Fallback, No API Needed)
Calculates straight-line distance between two lat/lng points accounting for Earth's curvature.

```java
public double haversine(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371; // Earth radius in km
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
             + Math.cos(Math.toRadians(lat1))
             * Math.cos(Math.toRadians(lat2))
             * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}
```

> Use this when the external API is down or rate-limited.

### Option B — Google Distance Matrix API (Real Road Distances)
Returns actual road distances and travel times — more accurate than straight-line.

```
GET https://maps.googleapis.com/maps/api/distancematrix/json
  ?origins=lat1,lon1|lat2,lon2
  &destinations=lat1,lon1|lat2,lon2
  &key=YOUR_API_KEY
```

### Option C — OpenStreetMap OSRM (Free Alternative)
```
GET http://router.project-osrm.org/table/v1/driving/
    lon1,lat1;lon2,lat2;lon3,lat3
    ?annotations=duration,distance
```
> Note: OSRM takes longitude FIRST, then latitude. Don't mix them up.

---

## 14. Greedy TSP Algorithm

### Step-by-Step Logic

```
1. Start at the depot (origin point)
2. Mark depot as visited
3. current = depot

4. While unvisited points remain:
   a. Find the unvisited point closest to current
   b. Add it to the route
   c. Mark it as visited
   d. current = that point

5. Return the ordered list
```

### Java Pseudocode

```java
public List<DeliveryTaskEntity> applyGreedyTSP(
        DeliveryTaskEntity depot,
        List<DeliveryTaskEntity> tasks,
        double[][] distanceMatrix) {

    List<DeliveryTaskEntity> ordered = new ArrayList<>();
    boolean[] visited = new boolean[tasks.size()];
    int current = 0; // depot index

    for (int i = 0; i < tasks.size(); i++) {
        int nearest = -1;
        double minDist = Double.MAX_VALUE;

        for (int j = 0; j < tasks.size(); j++) {
            if (!visited[j] && distanceMatrix[current][j] < minDist) {
                minDist = distanceMatrix[current][j];
                nearest = j;
            }
        }

        visited[nearest] = true;
        ordered.add(tasks.get(nearest));
        current = nearest;
    }

    return ordered;
}
```

### Example Run
```
Points: Depot, A(5km), B(8km from depot, 2km from C), C(3km from depot)

Step 1: At Depot → nearest is C (3km) → go to C
Step 2: At C → nearest unvisited is A (from C) → go to A
Step 3: At A → only B left → go to B

Result: Depot → C → A → B
Total: 3 + (C to A) + (A to B)
```

---

## 15. External API Integration

### RestTemplate Config
```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### DistanceMatrixConfig
```java
@Configuration
@ConfigurationProperties(prefix = "distance.api")
public class DistanceMatrixConfig {
    private String googleApiKey;
    private String googleBaseUrl;
    private String osrmBaseUrl;
}
```

### Calling Google Distance Matrix
```java
String url = googleBaseUrl + "?origins={origins}&destinations={destinations}&key={key}";
ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, origins, destinations, apiKey);
```

### Fallback Strategy
```java
try {
    return getDistanceMatrix(tasks); // external API
} catch (Exception e) {
    log.warn("External API failed, falling back to Haversine");
    return buildHaversineMatrix(tasks); // local calculation
}
```

---

## 16. Full Dispatch Flow — End to End

```
POST /api/dispatch/optimize
{
  "vehicleId": 1,
  "driverId": 2,
  "deliveryTaskIds": [10, 11, 12, 13, 14]
}

        ↓ DispatchController
        ↓ DispatchService.dispatchRoute()

Step 1: validateDispatchRequest()
        - Vehicle exists? → else ResourceNotFoundException
        - Driver exists? → else ResourceNotFoundException
        - Vehicle status == AVAILABLE? → else ValidationException
        - Driver status == AVAILABLE? → else ValidationException
        - Driver license valid? → else ValidationException

Step 2: fetchDeliveryWaypoints()
        - Load all DeliveryTaskEntity by IDs
        - Validate all have lat/lng
        - Validate all belong to this vehicle

Step 3: DistanceMatrixService.getDistanceMatrix()
        - Try Google/OSRM API
        - On failure → fall back to Haversine
        - Returns double[][] matrix

Step 4: RoutingService.applyGreedyTSP()
        - Input: distance matrix + task list
        - Output: ordered List<DeliveryTaskEntity>

Step 5: assignVehicleAndDriver()
        - Set vehicle status → IN_USE
        - Set driver status → ON_SHIFT

Step 6: RouteService.createRoute()
        - Save Route entity with status PLANNED

Step 7: buildOptimizedRouteResponse()
        - Map ordered tasks to OptimizedRouteResponseDTO
        - Calculate total distance
        - Return response

        ↓ Response

{
  "routeId": 99,
  "vehicleId": 1,
  "driverId": 2,
  "totalDistanceKm": 24.7,
  "status": "PLANNED",
  "stops": [
    { "sequence": 1, "address": "123 Anna Salai", "latitude": 13.05, "longitude": 80.24, "distanceFromPreviousKm": 0 },
    { "sequence": 2, "address": "456 Mount Road", "latitude": 13.06, "longitude": 80.25, "distanceFromPreviousKm": 3.2 },
    ...
  ]
}
```

---

## 17. Validation Rules

| Field | Rule |
|-------|------|
| Vehicle capacity | Must be > 0 |
| License plate | Must not be null or empty |
| Delivery latitude | Must not be null, between -90 and 90 |
| Delivery longitude | Must not be null, between -180 and 180 |
| Driver assignment | Driver must exist in DB before assignment |
| Driver license | `licenseValidUntil` must be after today's date |
| Dispatch | Vehicle must be AVAILABLE before dispatch |
| Dispatch | Driver must be AVAILABLE before dispatch |
| Shift hours | Must be > 0 and ≤ 24 |

---

## 18. Exception Handling

### GlobalExceptionHandler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<String> handleValidation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong");
    }
}
```

### Custom Exceptions
```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
```

---

## 19. application.properties Configuration

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/fleet_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

# External API
distance.api.google-api-key=YOUR_GOOGLE_API_KEY
distance.api.google-base-url=https://maps.googleapis.com/maps/api/distancematrix/json
distance.api.osrm-base-url=http://router.project-osrm.org/table/v1/driving

# App
server.port=8080
```

---

## 20. Week 1 vs Week 2 Scope

### Week 1 — You Are Here
- [x] DriverEntity + DriverRepository
- [ ] VehicleEntity, DeliveryTaskEntity, RouteEntity
- [ ] All Enums
- [ ] VehicleRepository, DeliveryTaskRepository, RouteRepository
- [ ] VehicleService, DeliveryTaskService, RouteService (basic CRUD only)
- [ ] VehicleController, DeliveryTaskController, RouteController
- [ ] Basic validation rules
- [ ] Exception handling setup
- [ ] application.properties + MySQL connection

### Week 2 — Coming Next
- [ ] DriverService + DriverController
- [ ] DistanceMatrixService (Haversine first, then external API)
- [ ] RoutingService (Greedy TSP)
- [ ] DispatchService (orchestrator)
- [ ] DispatchController
- [ ] All DTOs
- [ ] RestTemplate config
- [ ] Full dispatch flow end-to-end
- [ ] API fallback logic

---

## 21. Common Mistakes to Avoid

| Mistake | Why It's a Problem | Fix |
|---------|--------------------|-----|
| Using `@Enumerated(EnumType.ORDINAL)` | Stores 0,1,2 in DB — breaks if you reorder enum values | Always use `EnumType.STRING` |
| Skipping the depot/origin point | TSP has no start point — algorithm breaks | Always define a starting coordinate |
| Using Manhattan Distance for real coords | Only works for grid cities, not geographic coordinates | Use Haversine for lat/lng |
| No API fallback | If Google API is down, entire dispatch fails | Always implement Haversine as fallback |
| Returning entities directly from controllers | Exposes DB structure, causes circular reference with JPA relationships | Always use DTOs in controllers |
| OSRM coordinate order | OSRM takes `longitude,latitude` not `latitude,longitude` | Double-check coordinate order per API |
| Not validating before dispatch | Invalid driver/vehicle causes mid-flow crash | Always validate in Step 1 of dispatch |
| `WidthType.PERCENTAGE` in tables | Breaks layout in some renderers | Use `WidthType.DXA` always |
| Skipping `@Transactional` on service methods | DB state can be partially saved on error | Add `@Transactional` to write operations |
| Hardcoding API keys in code | Security risk | Always use `application.properties` + environment variables |

- [DESCRIPTION](Description.md)
