package com.example.fleet.controller;

import com.example.fleet.dto.DeliveryTaskRequestDTO;
import com.example.fleet.dto.DeliveryTaskResponseDTO;
import com.example.fleet.entity.Driver;
import com.example.fleet.entity.Vehicle;
import com.example.fleet.entity.enums.DeliveryStatus;
import com.example.fleet.entity.enums.VehicleStatus;
import com.example.fleet.service.DeliveryTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {
	@Autowired
	private DeliveryTaskService service;

	@PostMapping("/")
	public ResponseEntity<DeliveryTaskResponseDTO> createDeliveryTask(@RequestBody DeliveryTaskRequestDTO requestDTO){
		var responseDto = service.createDeliveryTask(requestDTO);
		return ResponseEntity.ok(responseDto);
	}

	@GetMapping("/")
	public ResponseEntity<List<DeliveryTaskResponseDTO>> getAllTasks(){
		var responseDto = service.getAllDeliverTasks();
		return ResponseEntity.ok(responseDto);
	}

	@GetMapping("/{id}")
	public ResponseEntity<DeliveryTaskResponseDTO> getTaskByID(@PathVariable Long id){
		var responseDto = service.getDeliveryTaskById(id);
		return ResponseEntity.ok(responseDto);
	}
	@PutMapping("/{id}/assign-vehicle")
	public ResponseEntity<DeliveryTaskResponseDTO> assaignTaskToVehilce(@PathVariable Long taskId ,@RequestBody Vehicle vehicle){
		var vehilceId= vehicle.getId();
		var responseDto = service.assignTaskToVehilce(vehilceId,taskId);
		return ResponseEntity.ok(responseDto);
	}

	@PutMapping("/{id}/assign-driver")
	public ResponseEntity<DeliveryTaskResponseDTO> assaignTaskToDriver(@PathVariable Long taskId ,@RequestBody Driver driver){
		var driverId= driver.getId();
		var responseDto = service.assignTaskToVehilce(driverId,taskId);
		return ResponseEntity.ok(responseDto);
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<DeliveryTaskResponseDTO> updateDeliveryStatus(@RequestBody DeliveryStatus status, @PathVariable Long id){
		var responseDto = service.updateDeliveryStatus(id , status);
		return ResponseEntity.ok(responseDto);
	}

	@PutMapping("/vehicle/{vehicleId}")
	public ResponseEntity<DeliveryTaskResponseDTO> getTaskByVehicle(){
		var responseDto = service.getTasksByVehicle();
		return ResponseEntity.ok(responseDto);
	}


}
