package com.example.fleet.service;

import com.example.fleet.dto.DeliveryTaskRequestDTO;
import com.example.fleet.dto.DeliveryTaskResponseDTO;
import com.example.fleet.entity.DeliveryTask;
import com.example.fleet.entity.Driver;
import com.example.fleet.entity.Vehicle;
import com.example.fleet.entity.enums.DeliveryStatus;
import com.example.fleet.entity.enums.DriverStatus;
import com.example.fleet.entity.enums.VehicleStatus;
import com.example.fleet.exception.ResourceNotFoundException;
import com.example.fleet.repository.DeliveryTaskRepository;
import com.example.fleet.repository.DriverRepository;
import com.example.fleet.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeliveryTaskService {
	@Autowired
	private DeliveryTaskRepository deliveryTaskRepository;
	private VehicleRepository vehicleRepository;
	private DriverRepository driverRepository;

	public DeliveryTaskService(DeliveryTaskRepository deliveryTaskRepository) {
		this.deliveryTaskRepository = deliveryTaskRepository;
	}

	public DeliveryTaskResponseDTO createDeliveryTask(DeliveryTaskRequestDTO requestDTO) {
		DeliveryTask deliveryTask = new DeliveryTask();
		deliveryTask.setStatus(requestDTO.getStatus());
		deliveryTask.setId(requestDTO.getId());
		deliveryTask.setAddress(requestDTO.getAddress());
		deliveryTask.setLatitude(requestDTO.getLatitude());
		deliveryTask.setVehicle(requestDTO.getVehicle());
		deliveryTask.setDriver(requestDTO.getDriver());
		deliveryTask.setLongitude(requestDTO.getLongitude());
		var saved = deliveryTaskRepository.save(deliveryTask);
		return new DeliveryTaskResponseDTO(
				saved.getId(),
				saved.getAddress(),
				saved.getLatitude(),
				saved.getLongitude(),
				saved.getStatus(),
				saved.getVehicle(),
				saved.getDriver()
		);
	}

	public DeliveryTaskResponseDTO getDeliveryTaskById(Long id) {
		DeliveryTask vehicle = deliveryTaskRepository.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));
		return new DeliveryTaskResponseDTO(
				vehicle.getId(),
				vehicle.getAddress(),
				vehicle.getLatitude(),
				vehicle.getLongitude(),
				vehicle.getStatus(),
				vehicle.getVehicle(),
				vehicle.getDriver()
		);
	}

	public List<DeliveryTaskResponseDTO> getAllDeliverTasks() {
		return deliveryTaskRepository.findAll().stream()
				.map(
						deliveryTasks -> new DeliveryTaskResponseDTO(
								deliveryTasks.getId(),
								deliveryTasks.getAddress(),
								deliveryTasks.getLatitude(),
								deliveryTasks.getLongitude(),
								deliveryTasks.getStatus(),
								deliveryTasks.getVehicle(),
								deliveryTasks.getDriver()
						)).toList();
	}

	public DeliveryTaskResponseDTO assignTaskToVehilce(Long vehicleId, Long taskId) {
		Vehicle vehicle = vehicleRepository.findById(vehicleId)
				.orElseThrow(() -> new RuntimeException("Vehicle not found"));
		DeliveryTask deliveryTask = deliveryTaskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));

		if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
			throw new IllegalStateException("Cannot assign a vehicle that is under maintenance");
		}
		if (deliveryTask.getStatus() == DeliveryStatus.ASSIGNED) {
			throw new IllegalStateException("Cannot assign this task as it is aldready assigned to some other driver ");
		}

		deliveryTask.setVehicle(vehicle);
		deliveryTask.setStatus(DeliveryStatus.ASSIGNED);
		vehicle.setStatus(VehicleStatus.IN_USE);
		deliveryTaskRepository.save(deliveryTask);
		DeliveryTask deliverytask = deliveryTaskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("DeliveryTask Not Found "));
		return  new DeliveryTaskResponseDTO(
				deliverytask.getId(),
				deliverytask.getAddress(),
				deliverytask.getLatitude(),
				deliverytask.getLongitude(),
				deliverytask.getStatus(),
				deliverytask.getVehicle(),
				deliverytask.getDriver()
		);
	}

	// TODO: we have DeliveryTaskStatus.ASSIGNED if the driver is not assinged and vehilce is assigned the status gets updated and might
// introduce bad behaviour
	public DeliveryTaskResponseDTO assignTaskToDriver(Long driverId, Long taskId) {
		Driver driver = driverRepository.findById(driverId)
				.orElseThrow(() -> new RuntimeException("Driver not found"));
		DeliveryTask deliveryTask = deliveryTaskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));

		if (driver.getStatus() == DriverStatus.ON_SHIFT) {
			throw new IllegalStateException("Cannot assign a driver that is under maintenance");
		}
		if (deliveryTask.getStatus() == DeliveryStatus.ASSIGNED) {
			throw new IllegalStateException("Cannot assign this task as it is aldready assigned to some other driver ");
		}

		deliveryTask.setDriver(driver);
		deliveryTask.setStatus(DeliveryStatus.ASSIGNED);
		// TODO : ONSHIFT Means he is not available but why do we need a .OFFSHIFT??
		driver.setStatus(DriverStatus.ON_SHIFT);
		deliveryTaskRepository.save(deliveryTask);
		DeliveryTask deliveryTask1 = deliveryTaskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
		return new DeliveryTaskResponseDTO(
				deliveryTask1.getId(),
				deliveryTask1.getAddress(),
				deliveryTask1.getLatitude(),
				deliveryTask1.getLongitude(),
				deliveryTask1.getStatus(),
				deliveryTask1.getVehicle(),
				deliveryTask1.getDriver()
		);
	}

	public List<DeliveryTaskResponseDTO> getTasksByVehicle(Long vehicleId) {
		List<DeliveryTask> vehilceTasks = new ArrayList<>();
		List<DeliveryTask> base = deliveryTaskRepository.findAll();
		for (DeliveryTask deliverytask : base) {
			if (deliverytask.getVehicle().getId().equals(vehicleId)) {
				vehilceTasks.add(deliverytask);
			}
		}
		return vehilceTasks.stream()
				.map(
						response -> new DeliveryTaskResponseDTO(
								response.getId(),
								response.getAddress(),
								response.getLatitude(),
								response.getLongitude(),
								response.getStatus(),
								response.getVehicle(),
								response.getDriver()
						)).toList();
	}

	public List<DeliveryTaskResponseDTO> getTasksByDriver(Long driverId) {
		List<DeliveryTask> deliveryList = new ArrayList<>();
		List<DeliveryTask> base = deliveryTaskRepository.findAll();
		for (DeliveryTask deliverytask : base) {
			if (deliverytask.getDriver().getId().equals(driverId)) {
				deliveryList.add(deliverytask);
			}
		}
		return deliveryList.stream()
				.map(
						response -> new DeliveryTaskResponseDTO(
								response.getId(),
								response.getAddress(),
								response.getLatitude(),
								response.getLongitude(),
								response.getStatus(),
								response.getVehicle(),
								response.getDriver()
						)).toList();
	}

	public DeliveryTaskResponseDTO updateDeliveryStatus(Long id , DeliveryStatus status){
		DeliveryTask deliveryTask = deliveryTaskRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Delivery Not Found "));
		deliveryTask.setStatus(status);
		deliveryTaskRepository.save(deliveryTask);
		return new DeliveryTaskResponseDTO(
				deliveryTask.getId(),
				deliveryTask.getAddress(),
				deliveryTask.getLatitude(),
				deliveryTask.getLongitude(),
				deliveryTask.getStatus(),
				deliveryTask.getVehicle(),
				deliveryTask.getDriver()
		);
	}
}