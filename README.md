# Fleet Management System — Services & Controllers Reference

---
- [DESCRIPTION](Description.md)
# Routing & Dispatch Engine — Method Reference

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