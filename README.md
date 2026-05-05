# Fleet Management System — Services & Controllers Reference

---

## Services

### DriverService
- `registerDriver`
- `getDriverById`
- `getAllDrivers`
- `updateDriverShiftHours`
- `validateDriverLicense`
- `isDriverAvailable`

---

### VehicleService
- `registerVehicle`
- `getVehicleById`
- `getAllVehicles`
- `getAvailableVehicles`
- `assignDriverToVehicle`
- `updateVehicleStatus`
- `updateMaintenanceDate`
- `validateVehicleCapacity`

---

### DeliveryTaskService
- `createDeliveryTask`
- `getDeliveryTaskById`
- `getAllDeliveryTasks`
- `assignTaskToVehicle`
- `assignTaskToDriver`
- `updateDeliveryStatus`
- `getTasksByVehicle`
- `getTasksByDriver`
- `validateCoordinates`

---

### RouteService
- `createRoute`
- `getRouteById`
- `getAllRoutes`
- `getRoutesByVehicle`
- `getRoutesByDriver`
- `updateRouteStatus`
- `getActiveRoutes`

---

### RoutingService *(TSP Optimization)*
- `optimizeRoute`
- `applyGreedyTSP`
- `calculateTotalDistance`
- `buildWaypointMatrix`
- `sequenceWaypoints`

---

### DistanceMatrixService *(External API)*
- `getDistanceMatrix`
- `calculateDistanceBetweenPoints`
- `buildApiRequestPayload`
- `parseApiResponse`
- `handleApiFailure`

---

### DispatchService *(Orchestrator)*
- `dispatchRoute`
- `validateDispatchRequest`
- `assignVehicleAndDriver`
- `fetchDeliveryWaypoints`
- `buildOptimizedRouteResponse`

---

## Controllers

### DriverController — `/api/drivers`
| Method | Endpoint | Handler |
|--------|----------|---------|
| POST | `/` | `registerDriver` |
| GET | `/` | `getAllDrivers` |
| GET | `/{id}` | `getDriverById` |
| PUT | `/{id}/shift-hours` | `updateDriverShiftHours` |
| GET | `/{id}/availability` | `isDriverAvailable` |

---

### VehicleController — `/api/vehicles`
| Method | Endpoint | Handler |
|--------|----------|---------|
| POST | `/` | `registerVehicle` |
| GET | `/` | `getAllVehicles` |
| GET | `/{id}` | `getVehicleById` |
| GET | `/available` | `getAvailableVehicles` |
| PUT | `/{id}/status` | `updateVehicleStatus` |
| PUT | `/{id}/assign-driver` | `assignDriverToVehicle` |
| PUT | `/{id}/maintenance` | `updateMaintenanceDate` |

---

### DeliveryTaskController — `/api/deliveries`
| Method | Endpoint | Handler |
|--------|----------|---------|
| POST | `/` | `createDeliveryTask` |
| GET | `/` | `getAllDeliveryTasks` |
| GET | `/{id}` | `getDeliveryTaskById` |
| PUT | `/{id}/assign-vehicle` | `assignTaskToVehicle` |
| PUT | `/{id}/assign-driver` | `assignTaskToDriver` |
| PUT | `/{id}/status` | `updateDeliveryStatus` |
| GET | `/vehicle/{vehicleId}` | `getTasksByVehicle` |
| GET | `/driver/{driverId}` | `getTasksByDriver` |

---

### RouteController — `/api/routes`
| Method | Endpoint | Handler |
|--------|----------|---------|
| POST | `/` | `createRoute` |
| GET | `/` | `getAllRoutes` |
| GET | `/{id}` | `getRouteById` |
| GET | `/vehicle/{vehicleId}` | `getRoutesByVehicle` |
| GET | `/driver/{driverId}` | `getRoutesByDriver` |
| PUT | `/{id}/status` | `updateRouteStatus` |
| GET | `/active` | `getActiveRoutes` |

---

### DispatchController — `/api/dispatch`
| Method | Endpoint | Handler |
|--------|----------|---------|
| POST | `/optimize` | `dispatchRoute` |
| GET | `/status/{routeId}` | `getDispatchStatus` |
| POST | `/validate` | `validateDispatchRequest` |