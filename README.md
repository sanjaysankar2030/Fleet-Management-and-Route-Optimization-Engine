# Fleet Management System — Week 1 Implementation Guide

## Objective

Complete the foundational backend for the Fleet Management system by implementing:

* Core entities
* Database mappings
* Repository layer
* Basic API endpoints

**Scope constraint:** No routing logic, no external APIs, no optimization. Only data modeling + CRUD.

---

## What is Already Done

* `DriverEntity`
* `DriverRepository`

---

## What You Need to Implement

### 1. Vehicle Entity

#### Fields

* `id` (Primary Key)
* `licensePlate` (String, unique)
* `capacity` (double)
* `status` (Enum: AVAILABLE, IN_USE, MAINTENANCE)
* `lastMaintenanceDate` (LocalDate)

#### Requirements

* Use `@Entity`
* Use `@Id` with auto-generation
* Use `@Enumerated(EnumType.STRING)` for status
* Add Lombok annotations (`@Getter`, `@Setter`, etc.)

---

### 2. DeliveryTask Entity

#### Fields

* `id`
* `address`
* `latitude`
* `longitude`
* `status` (Enum: UNASSIGNED, ASSIGNED, DELIVERED)

#### Relationships

* Many-to-One → Vehicle
* Many-to-One → Driver

#### Notes

* Do NOT overcomplicate with Route yet
* Keep mapping simple

---

### 3. Route Entity (Basic Version)

#### Fields

* `id`
* `date` (LocalDate)
* `status` (Enum: PLANNED, ACTIVE, COMPLETED)

#### Relationships

* Many-to-One → Vehicle
* Many-to-One → Driver

#### Important

* Do NOT add delivery sequencing logic yet
* This is just a placeholder structure for Week 2

---

## Enum Definitions (Create Separate Package)

Create a package:

```
entity.enums
```

Define:

### VehicleStatus

* AVAILABLE
* IN_USE
* MAINTENANCE

### DeliveryStatus

* UNASSIGNED
* ASSIGNED
* DELIVERED

### RouteStatus

* PLANNED
* ACTIVE
* COMPLETED

---

## Repository Layer

Create interfaces extending `JpaRepository`:

```
VehicleRepository
DeliveryTaskRepository
RouteRepository
```

Example:

```java
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}
```

---

## Service Layer (MANDATORY)

Do NOT skip this.

Create:

```
VehicleService
DeliveryTaskService
RouteService
```

Responsibilities:

* Business validation
* State updates
* Data orchestration

### Example Responsibilities

#### VehicleService

* Register vehicle
* Fetch available vehicles
* Assign driver (basic)

#### DeliveryTaskService

* Create delivery task
* Assign to vehicle/driver

---

## Controller Layer

Create REST endpoints:

```
/api/vehicles
/api/deliveries
/api/routes
```

### Minimum APIs Required

#### Vehicle

* POST `/api/vehicles` → create
* GET `/api/vehicles` → list
* GET `/api/vehicles/available` → filter

#### DeliveryTask

* POST `/api/deliveries`
* GET `/api/deliveries`

#### Route

* POST `/api/routes`
* GET `/api/routes`

---

## Database Configuration

Update `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/fleet_db
spring.datasource.username=root
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

## Validation Rules (Basic)

Implement at service level:

* Vehicle capacity must be > 0
* License plate must not be empty
* Delivery coordinates must not be null
* Driver must exist before assignment

---

## Folder Structure (STRICT)

```
com.example.fleet
│
├── controller/
├── service/
├── repository/
├── entity/
│   └── enums/
├── dto/ (optional)
├── exception/ (optional)
```

---

## What NOT to Do

* No routing algorithms
* No external API calls
* No premature optimization
* No business logic in controllers
* No string-based status fields

---

## Deliverables by End of Week 1

* All 4 entities implemented
* Proper relationships defined
* CRUD APIs working
* Data persists correctly in MySQL
* Clean project structure

---

## Quality Check (Self Review)

Before submitting, verify:

* Can create a vehicle via API
* Can create a delivery task
* Relationships are saved correctly
* Enums are stored as strings in DB
* No logic in controller layer

---

## Final Note

If your implementation:

* mixes layers
* skips relationships
* hardcodes values

then Week 2 (routing engine) will fail.

Keep this clean and minimal.
