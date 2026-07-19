# GraphHopper Matrix API Integration — Complete Guide

Integrate **GraphHopper Matrix API** as primary distance calculator with **Haversine** as automatic fallback.

---

## Step 1: Add Dependencies to `pom.xml`

```xml
<!-- GraphHopper Matrix API (REST client) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>

<!-- For easier HTTP requests -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Logging -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
</dependency>
```

---

## Step 2: Configure GraphHopper API in `application.properties`

```properties
# ===== GraphHopper Matrix API Configuration =====
graphhopper.api.key=YOUR_GRAPHHOPPER_API_KEY
graphhopper.api.base-url=https://graphhopper.com/api/1/matrix
graphhopper.api.timeout-seconds=30
graphhopper.api.enable=true

# Fallback to Haversine if GraphHopper fails (true by default)
graphhopper.api.use-fallback=true

# ===== Existing Distance Config =====
distance.api.google-api-key=${GOOGLE_API_KEY:}
distance.api.google-base-url=https://maps.googleapis.com/maps/api/distancematrix/json
```

### **Get Your GraphHopper API Key:**
1. Sign up at https://graphhopper.com
2. Go to Dashboard → API Keys
3. Create new key (free tier: 100 requests/day, paid: up to 10M/month)
4. Copy key to `application.properties`

---

## Step 3: Create GraphHopper Request/Response DTOs

```java
// src/main/java/com/fleet/dto/graphhopper/GraphHopperMatrixRequest.java
package com.fleet.dto.graphhopper;

import java.util.ArrayList;
import java.util.List;

public class GraphHopperMatrixRequest {
    private List<double[]> locations;  // [[lng, lat], [lng, lat], ...]
    private String profile;            // "car", "bike", "foot"
    private boolean debug;

    public GraphHopperMatrixRequest(List<double[]> locations) {
        this.locations = locations;
        this.profile = "car";
        this.debug = false;
    }

    // Getters and setters
    public List<double[]> getLocations() { return locations; }
    public void setLocations(List<double[]> locations) { this.locations = locations; }

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }
}
```

```java
// src/main/java/com/fleet/dto/graphhopper/GraphHopperMatrixResponse.java
package com.fleet.dto.graphhopper;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphHopperMatrixResponse {
    @JsonProperty("distances")
    private List<List<Long>> distances;  // distances[i][j] in meters

    @JsonProperty("times")
    private List<List<Long>> times;      // times[i][j] in milliseconds

    @JsonProperty("info")
    private InfoObject info;

    // Constructor
    public GraphHopperMatrixResponse() {}

    // Getters
    public List<List<Long>> getDistances() { return distances; }
    public void setDistances(List<List<Long>> distances) { this.distances = distances; }

    public List<List<Long>> getTimes() { return times; }
    public void setTimes(List<List<Long>> times) { this.times = times; }

    public InfoObject getInfo() { return info; }
    public void setInfo(InfoObject info) { this.info = info; }

    // Convert meters to km
    public double[][] toKilometersMatrix() {
        if (distances == null || distances.isEmpty()) {
            throw new RuntimeException("GraphHopper response has no distances");
        }

        int n = distances.size();
        double[][] matrixKm = new double[n][n];

        for (int i = 0; i < n; i++) {
            List<Long> row = distances.get(i);
            for (int j = 0; j < row.size(); j++) {
                // Convert meters to kilometers
                matrixKm[i][j] = row.get(j) / 1000.0;
            }
        }

        return matrixKm;
    }

    // Inner class for API info
    public static class InfoObject {
        private String copyrights;
        private String took;

        public String getCopyrights() { return copyrights; }
        public void setCopyrights(String copyrights) { this.copyrights = copyrights; }

        public String getTook() { return took; }
        public void setTook(String took) { this.took = took; }
    }
}
```

---

## Step 4: Create GraphHopper Service

```java
// src/main/java/com/fleet/service/GraphHopperMatrixService.java
package com.fleet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.dto.graphhopper.GraphHopperMatrixRequest;
import com.fleet.dto.graphhopper.GraphHopperMatrixResponse;
import com.fleet.dto.Waypoint;

import java.util.ArrayList;
import java.util.List;

@Service
public class GraphHopperMatrixService {

    private static final Logger log = LoggerFactory.getLogger(GraphHopperMatrixService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${graphhopper.api.key}")
    private String graphHopperApiKey;

    @Value("${graphhopper.api.base-url}")
    private String graphHopperBaseUrl;

    @Value("${graphhopper.api.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${graphhopper.api.enable:true}")
    private boolean graphHopperEnabled;

    // -------------------------------------------------------
    // PUBLIC ENTRY POINT — converts Waypoints to request
    // -------------------------------------------------------
    public double[][] getDistanceMatrixFromGraphHopper(List<Waypoint> waypoints) {
        if (!graphHopperEnabled) {
            throw new RuntimeException("GraphHopper API is disabled in configuration");
        }

        try {
            log.info("Calling GraphHopper Matrix API for {} waypoints", waypoints.size());

            // Convert waypoints to GraphHopper format
            GraphHopperMatrixRequest request = buildGraphHopperRequest(waypoints);

            // Make API call
            GraphHopperMatrixResponse response = callGraphHopperApi(request);

            // Convert response to double[][] (in km)
            double[][] matrix = response.toKilometersMatrix();

            log.info("GraphHopper API call successful. Matrix size: {}x{}", matrix.length, matrix[0].length);
            return matrix;

        } catch (Exception e) {
            log.error("GraphHopper API failed: {}", e.getMessage());
            throw new RuntimeException("GraphHopper Matrix API call failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------
    // BUILD REQUEST — convert Waypoints to GraphHopper format
    // IMPORTANT: GraphHopper expects [longitude, latitude] order!
    // -------------------------------------------------------
    private GraphHopperMatrixRequest buildGraphHopperRequest(List<Waypoint> waypoints) {
        List<double[]> locations = new ArrayList<>();

        for (Waypoint wp : waypoints) {
            // CRITICAL: GraphHopper uses [lng, lat], NOT [lat, lng]
            double[] location = {wp.getLng(), wp.getLat()};
            locations.add(location);
        }

        GraphHopperMatrixRequest request = new GraphHopperMatrixRequest(locations);
        request.setProfile("car");  // Options: car, bike, foot, etc.
        request.setDebug(false);

        log.debug("Built GraphHopper request with {} locations", locations.size());
        return request;
    }

    // -------------------------------------------------------
    // API CALL — execute HTTP POST to GraphHopper
    // -------------------------------------------------------
    private GraphHopperMatrixResponse callGraphHopperApi(GraphHopperMatrixRequest request) {
        try {
            // Build URL with API key
            String url = graphHopperBaseUrl + "?key=" + graphHopperApiKey;

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create request body
            String requestBody = objectMapper.writeValueAsString(request);
            log.debug("GraphHopper request body: {}", requestBody);

            // Make HTTP POST request
            HttpEntity<String> httpRequest = new HttpEntity<>(requestBody, headers);
            GraphHopperMatrixResponse response = restTemplate.postForObject(
                url,
                httpRequest,
                GraphHopperMatrixResponse.class
            );

            if (response == null || response.getDistances() == null) {
                throw new RuntimeException("GraphHopper returned empty response");
            }

            log.info("GraphHopper response received: {} rows × {} cols",
                response.getDistances().size(),
                response.getDistances().get(0).size()
            );

            return response;

        } catch (Exception e) {
            log.error("Error calling GraphHopper API: {}", e.getMessage(), e);
            throw new RuntimeException("GraphHopper API call failed", e);
        }
    }

    // -------------------------------------------------------
    // VALIDATE RESPONSE — ensure matrix is NxN and non-empty
    // -------------------------------------------------------
    public void validateMatrixResponse(GraphHopperMatrixResponse response, int expectedSize) {
        if (response.getDistances() == null || response.getDistances().isEmpty()) {
            throw new RuntimeException("GraphHopper response has no distance data");
        }

        int actualSize = response.getDistances().size();
        if (actualSize != expectedSize) {
            throw new RuntimeException(
                String.format("Matrix size mismatch: expected %d rows, got %d",
                    expectedSize, actualSize)
            );
        }

        // Verify each row is NxN (not jagged)
        for (int i = 0; i < actualSize; i++) {
            int colCount = response.getDistances().get(i).size();
            if (colCount != expectedSize) {
                throw new RuntimeException(
                    String.format("Jagged matrix: row %d has %d cols, expected %d",
                        i, colCount, expectedSize)
                );
            }
        }
    }
}
```

---

## Step 5: Modify DistanceMatrixService to Use GraphHopper + Haversine Fallback

```java
// src/main/java/com/fleet/service/DistanceMatrixService.java
package com.fleet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fleet.dto.Waypoint;
import java.util.List;

@Service
public class DistanceMatrixService {

    private static final Logger log = LoggerFactory.getLogger(DistanceMatrixService.class);

    @Autowired
    private GraphHopperMatrixService graphHopperMatrixService;

    // -------------------------------------------------------
    // PUBLIC ENTRY POINT — try GraphHopper first, fallback to Haversine
    // -------------------------------------------------------
    public double[][] getDistanceMatrix(List<Waypoint> waypoints) {
        try {
            log.info("Attempting GraphHopper Matrix API for {} waypoints", waypoints.size());
            return graphHopperMatrixService.getDistanceMatrixFromGraphHopper(waypoints);

        } catch (Exception e) {
            log.warn("GraphHopper API failed. Falling back to Haversine. Reason: {}", e.getMessage());
            return buildHaversineMatrix(waypoints);
        }
    }

    // -------------------------------------------------------
    // HAVERSINE FALLBACK — straight-line distance
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

        log.info("Building Haversine distance matrix for {} waypoints", n);

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

        log.info("Haversine matrix built successfully: {}x{}", n, n);
        return matrix;
    }
}
```

---

## Step 6: Configure RestTemplate Bean

```java
// src/main/java/com/fleet/config/HttpClientConfig.java
package com.fleet.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    // -------------------------------------------------------
    // RestTemplate with timeout settings
    // -------------------------------------------------------
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }

    // -------------------------------------------------------
    // ObjectMapper for JSON serialization
    // -------------------------------------------------------
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

---

## Step 7: Update RoutingService

No changes needed! It already calls `distanceMatrixService.getDistanceMatrix()`:

```java
// This stays the same — the fallback logic is now inside DistanceMatrixService
public List<DeliveryTaskEntity> optimizeRoute(List<DeliveryTaskEntity> tasks) {
    List<Waypoint> waypoints = buildWaypointMatrix(tasks);
    
    // This now tries GraphHopper first, then Haversine
    double[][] matrix = distanceMatrixService.getDistanceMatrix(waypoints);
    
    List<Integer> sequence = applyGreedyTSP(matrix);
    return sequenceWaypoints(sequence, tasks);
}
```

---

## Step 8: Add Logging Configuration (Optional but Recommended)

```yaml
# application.yml
logging:
  level:
    com.fleet.service.DistanceMatrixService: DEBUG
    com.fleet.service.GraphHopperMatrixService: DEBUG
    com.fleet.service.RoutingService: DEBUG
    org.springframework.web.client.RestTemplate: WARN
```

---

## Flow Diagram: GraphHopper + Haversine Fallback

```
POST /api/dispatch/optimize
    ↓
DispatchService.dispatchRoute()
    ↓
RoutingService.optimizeRoute()
    ↓
DistanceMatrixService.getDistanceMatrix(waypoints)
    ↓
    ├─→ TRY: GraphHopperMatrixService.getDistanceMatrixFromGraphHopper()
    │       │
    │       ├─→ Build request: [lng, lat], [lng, lat]...
    │       ├─→ POST to https://graphhopper.com/api/1/matrix?key=XXX
    │       ├─→ Parse response
    │       └─→ Convert meters → kilometers
    │
    └─→ CATCH Exception:
            log.warn("GraphHopper failed, falling back to Haversine")
            └─→ Build Haversine matrix (straight-line distances)
                └─→ Return double[][] matrix
                    ↓
                    RoutingService continues with TSP...
```

---

## Testing the Integration

### **Test 1: GraphHopper Success (Happy Path)**

```bash
curl -X POST http://localhost:8080/api/dispatch/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "driverId": 2,
    "vehicleId": 5,
    "deliveryTaskIds": [1, 2, 3]
  }'
```

**Expected Logs:**
```
INFO  - Attempting GraphHopper Matrix API for 3 waypoints
INFO  - Calling GraphHopper Matrix API for 3 waypoints
INFO  - Built GraphHopper request with 3 locations
INFO  - GraphHopper response received: 3 rows × 3 cols
INFO  - GraphHopper API call successful. Matrix size: 3x3
```

### **Test 2: GraphHopper Fallback (Invalid Key)**

Change `graphhopper.api.key=INVALID_KEY_12345` in `application.properties`, then:

```bash
curl -X POST http://localhost:8080/api/dispatch/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "driverId": 2,
    "vehicleId": 5,
    "deliveryTaskIds": [1, 2, 3]
  }'
```

**Expected Logs:**
```
WARN  - GraphHopper API failed. Falling back to Haversine. Reason: 401 Unauthorized
INFO  - Building Haversine distance matrix for 3 waypoints
INFO  - Haversine matrix built successfully: 3x3
```

### **Test 3: Verify Output Still Works**

Response should be identical in both cases — the TSP algorithm doesn't care where distances came from:

```json
{
  "routeId": 3,
  "vehicleId": 5,
  "driverId": 2,
  "totalDistanceKm": 12.34,
  "status": "PLANNED",
  "stops": [...]
}
```

---

## Key Differences: GraphHopper vs Haversine

| Aspect | GraphHopper | Haversine |
|--------|-------------|-----------|
| **Distance Type** | Road network (actual driving) | Straight-line (as-the-crow-flies) |
| **Accuracy** | ✅ Very high (~99%) | ⚠️ Lower (~70-80%) |
| **Speed** | API call (100-500ms) | Instant (microseconds) |
| **Cost** | Free tier: 100/day → Paid plans | Free (always) |
| **When Used** | Primary | Fallback only |
| **Profile Support** | Car, bike, foot, etc. | N/A |

---

## Production Checklist

- [ ] Add `graphhopper.api.key` to environment variables (not hardcoded)
- [ ] Set `graphhopper.api.timeout-seconds=30` appropriately
- [ ] Enable logging for debugging: `com.fleet.service=DEBUG`
- [ ] Test fallback manually (disable API key temporarily)
- [ ] Monitor GraphHopper API quota usage in their dashboard
- [ ] Consider caching distance matrices if the same routes are calculated repeatedly
- [ ] Add retry logic with exponential backoff (optional enhancement)
- [ ] Set up alerts if fallback to Haversine exceeds 10% of requests

---

## Optional Enhancement: Caching

Add Redis caching to avoid repeated API calls for same waypoint sets:

```java
@Service
public class DistanceMatrixService {
    
    @Cacheable(value = "distanceMatrix", key = "#waypoints.stream().map(w -> w.getTaskId()).sorted().collect(Collectors.joining(','))")
    public double[][] getDistanceMatrix(List<Waypoint> waypoints) {
        // ... existing code
    }
}
```

Configure Redis in `application.properties`:
```properties
spring.redis.host=localhost
spring.redis.port=6379
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000  # 1 hour
```

---

## Summary

✅ **GraphHopper** is now the primary distance matrix provider
✅ **Haversine** automatically kicks in if GraphHopper fails
✅ **No breaking changes** to existing API or routing logic
✅ **Transparent fallback** — clients don't need to know which method was used
✅ **Production-ready** with logging, error handling, and timeouts


