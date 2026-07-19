package com.example.fleet.service;

import com.example.fleet.dto.*;
import com.example.fleet.entity.*;
import com.example.fleet.entity.enums.DriverStatus;
import com.example.fleet.entity.enums.RouteStatus;
import com.example.fleet.entity.enums.VehicleStatus;
import com.example.fleet.exception.ResourceNotFoundException;
import com.example.fleet.repository.DeliveryTaskRepository;
import com.example.fleet.repository.DriverRepository;
import com.example.fleet.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

@Service
public class DispatchService {
	@Autowired
	private DeliveryTaskRepository deliveryTaskRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private DriverRepository driverRepository;

	@Autowired
	private DistMatrixService distanceMatrixService;

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
		List<DeliveryTask> tasks = fetchDeliveryWaypoints(request.getDeliveryListIds());

		// Step 3 + 4: build matrix and run TSP (both inside RoutingService)
		List<DeliveryTask> orderedTasks = routingService.optimizeRoute(tasks);

		// Step 5: calculate total distance for the final sequence
		List<WayPoint> waypoints = routingService.buildWaypointMatrix(tasks);
		double[][] matrix = distanceMatrixService.getDistanceMatrix(waypoints);
		List<Integer> sequence = routingService.applyGreedyTSP(matrix);
		double totalDistance = routingService.calculateTotalDistance(sequence, matrix);

		// Step 6: mark vehicle as IN_USE, driver as ON_SHIFT
		assignVehicleAndDriver(request.getVehicleId(), request.getDriverId());

		// Step 7: save a Route record
		RouteRequestEntity newRoute = new RouteRequestEntity(
								request.getId(),
								LocalDate.now(),
								RouteStatus.PLANNED,
								request.getVehicleId(),
								request.getDriverId()
				);
		RouteResponseEntity savedRoute = routeService.createRoute(newRoute);

		// Step 8: build and return response DTO
		return buildOptimizedRouteResponse(savedRoute.getId(), request.getVehicleId(),
				request.getDriverId(), orderedTasks, totalDistance, matrix, sequence);
	}

	// -------------------------------------------------------
	// STEP 1 — validate vehicle + driver exist and are free
	// -------------------------------------------------------
	public void validateDispatchRequest(DispatchRequestDTO request) {
		Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
				.orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.getVehicleId()));

		Driver driver = driverRepository.findById(request.getDriverId())
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

		int taskCount = request.getDeliveryListIds().size();
		if (taskCount < 1 || taskCount > 10) {
			throw new ValidationException("Task count must be between 1 and 10. Got: " + taskCount);
		}
	}

	// -------------------------------------------------------
	// STEP 2 — pull tasks from DB, check coordinates exist
	// -------------------------------------------------------
	public List<DeliveryTask> fetchDeliveryWaypoints(List<Long> taskIds) {
		List<DeliveryTask> tasks = deliveryTaskRepository.findAllById(taskIds);

		if (tasks.size() != taskIds.size()) {
			throw new ResourceNotFoundException("One or more delivery tasks not found");
		}

		for (DeliveryTask task : tasks) {
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
		Vehicle vehicle = vehicleRepository.findById(vehicleId)
				.orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
		vehicle.setStatus(VehicleStatus.IN_USE);
		vehicleRepository.save(vehicle);

		Driver driver = driverRepository.findById(driverId)
				.orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
		driver.setStatus(DriverStatus.ON_SHIFT);
		driverRepository.save(driver);
	}

	// -------------------------------------------------------
	// STEP 8 — assemble the final response DTO
	// -------------------------------------------------------
	public OptimizedRouteResponseDTO buildOptimizedRouteResponse(
			Long routeId, Long vehicleId, Long driverId,
			List<DeliveryTask> orderedTasks,
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
			DeliveryTask task = orderedTasks.get(i);
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



