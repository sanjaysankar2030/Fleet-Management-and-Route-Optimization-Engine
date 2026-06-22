package com.example.fleet.controller;

import com.example.fleet.dto.VehicleRequestDTO;
import com.example.fleet.dto.VehicleResponseDTO;
import com.example.fleet.entity.enums.VehicleStatus;
import com.example.fleet.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/vehicle")
public class VehicleController {
	@Autowired
	private VehicleService vehicleService;

	@GetMapping("/")
	public ResponseEntity<List<VehicleResponseDTO>> getAllVehicles(){
		var vehicles = vehicleService.getAllVehicles();
		return ResponseEntity.ok(vehicles);
	}

	//TODO : LastMaintainence date is serialized and stored as a 'date' object in mysql
	// But the deserialization of the date value gives null hence it cannot be sent for
	// the frontend to view through ResponseEntity<T>
	@PostMapping("/register")
	public ResponseEntity<VehicleResponseDTO> registerVehicle(@RequestBody VehicleRequestDTO request){
		var response = vehicleService.registerVehicle(request);
		return ResponseEntity.ok().body(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<VehicleResponseDTO> getVehicleByid(@PathVariable Long id){
		var vehicle = vehicleService.getVehicleById(id);
		return ResponseEntity.ok(vehicle);
	}

	@PatchMapping ("/{id}/status")
	public ResponseEntity<VehicleResponseDTO> updateVehicleStatus(
			@PathVariable Long id,
			@RequestBody VehicleStatus status
			// TODO : Keeping the enum in the body there may be good ways to do it too
	){
		vehicleService.updateVehicleStatus(id , status);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping ("/{id}/maintenance")
	public ResponseEntity<VehicleResponseDTO> updateMaintenanceDate(
			@PathVariable Long id,
			@RequestBody LocalDate date
	){
		vehicleService.updateMaintenanceDate(id , date);
		return ResponseEntity.noContent().build();
	}
	@GetMapping("avalilable")
	public ResponseEntity<List<VehicleResponseDTO>> getAvailableVehicle(){
		var available_vehicles = vehicleService.getAllAvailableVehilce();
		return ResponseEntity.ok(available_vehicles);
	}
	//	TODO: Assign Driver to Vehicle One to One relationship
	@GetMapping("/{vehicleId}/assign/{driverId}")
	public ResponseEntity<VehicleResponseDTO> assignDrivertoVehicle(
			@PathVariable Long vehicleId,
			@PathVariable Long driverId
	){
		var vehicle = vehicleService.assignDriverToVehicle(vehicleId,driverId);
		var vehicleDto = new VehicleResponseDTO(vehicle);
		return ResponseEntity.ok(vehicleDto);
	}

}
