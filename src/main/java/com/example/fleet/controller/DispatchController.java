package com.example.fleet.controller;

import com.example.fleet.dto.DispatchRequestDTO;
import com.example.fleet.dto.OptimizedRouteResponseDTO;
import com.example.fleet.dto.RouteResponseEntity;
import com.example.fleet.service.DispatchService;
import com.example.fleet.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {

	@Autowired
	private DispatchService dispatchService;

	@Autowired
	private RouteService routeService;

	// POST /api/dispatch/optimize
	@PostMapping("/optimize")
	public ResponseEntity<OptimizedRouteResponseDTO> dispatchRoute(
			@RequestBody DispatchRequestDTO request) {
		OptimizedRouteResponseDTO response = dispatchService.dispatchRoute(request);
		return ResponseEntity.ok(response);
	}

	// GET /api/dispatch/status/{routeId}
	@GetMapping("/status/{routeId}")
	public ResponseEntity<RouteResponseEntity> getDispatchStatus(@PathVariable Long routeId) {
		RouteResponseEntity route = routeService.getRouteById(routeId);
		return ResponseEntity.ok(route);
	}

	// POST /api/dispatch/validate
	@PostMapping("/validate")
	public ResponseEntity<String> validateDispatchRequest(
			@RequestBody DispatchRequestDTO request) {
		dispatchService.validateDispatchRequest(request);
		return ResponseEntity.ok("Dispatch request is valid");
	}
}