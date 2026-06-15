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
	public VehicleService vehicleService;

	@GetMapping("/")
	public ResponseEntity<List<VehicleResponseDTO>> getAllVehicles(){
		var vehicles = vehicleService.getAllVehicles();
		return ResponseEntity.ok(vehicles);
	}

	@PutMapping("/register")
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
	public ResponseEntity<VehicleResponseDTO> updateMaintenanceDate(){}
			@PathVariable Long id,
			@RequestBody LocalDate date
	){
		vehicleService.updateMaintenanceDate(id , date);
		return ResponseEntity.noContent().build();
	}
//	TODO: Assign Driver to Vehicle One to One relationship
}
