package com.example.fleet.service;

import com.example.fleet.entity.DeliveryTask;
import com.example.fleet.exception.ResourceNotFoundException;
import com.example.fleet.repository.DeliveryTaskRepository;
import com.example.fleet.repository.DriverRepository;
import com.example.fleet.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
	private DistMatrixService distMatrixService;

	@Autowired
	private RoutingService routingService;

	@Autowired
	private RouteService service;

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
}
