package com.example.fleet.controller;

import java.util.List;
import java.util.Map;

import com.example.fleet.dto.DriverRequestDTO;
import com.example.fleet.dto.DriverResponseDTO;
import com.example.fleet.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/driver")
public class DriverController {
	@Autowired
	public DriverService driverService;

	@PostMapping("/register")
	public ResponseEntity<DriverResponseDTO> registerDriver(@RequestBody DriverRequestDTO request) {
		var response = driverService.registerDriver(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/")
	public ResponseEntity<List<DriverResponseDTO>> getAllDrivers() {
		var drivers = driverService.getAllDrivers();
		return ResponseEntity.ok(drivers);
	}

	@GetMapping("/{id}")
	public ResponseEntity<DriverResponseDTO> getDriverById(@PathVariable Long id) {
		var driver = driverService.getDriverById(id);
		return ResponseEntity.ok(driver);
	}

	@PatchMapping("/{id}/shiftHours")
	public ResponseEntity<DriverResponseDTO> updateShiftHours(@PathVariable Long id,
															  @RequestParam double shiftHours){
		driverService.updateDriverShiftHours(id,shiftHours);
//		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/available")
	public ResponseEntity<Map<String, Boolean>> isDriverAvailable(@PathVariable Long id) {
		boolean available = driverService.isDriverAvailable(id);
		return ResponseEntity.ok(Map.of("available", available));
	}

}
