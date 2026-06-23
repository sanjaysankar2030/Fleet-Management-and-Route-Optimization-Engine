package com.example.fleet.controller;

import com.example.fleet.dto.RouteRequestEntity;
import com.example.fleet.dto.RouteResponseEntity;
import com.example.fleet.entity.enums.RouteStatus;
import com.example.fleet.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/route")
public class RouteController {
	@Autowired
	private RouteService service;
	@PostMapping("/")
	public ResponseEntity<RouteResponseEntity> createRoute(@RequestBody RouteRequestEntity request){
		var response = service.createRoute(request);
		return ResponseEntity.ok(response);
	}
	@GetMapping("/all")
	public ResponseEntity<List<RouteResponseEntity>> getAllRoutes(){
		var response =  service.getAllRoutes();
		return ResponseEntity.ok(response);
	}
	@GetMapping("/{id}")
	public ResponseEntity<RouteResponseEntity> getRouteById(@PathVariable Long id){
		var response = service.getRouteById(id);
		return ResponseEntity.ok(response);
	}
	@GetMapping("/vehicle/{vehicleId}")
	public  ResponseEntity<List<RouteResponseEntity>> getRoutesByVehilce(@PathVariable Long id ){
		var response = service.getRouteByVehicle(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/driver/{driverId}")
	public  ResponseEntity<List<RouteResponseEntity>> getRoutesByDriver(@PathVariable Long id ){
		var response = service.getRouteByDriver(id);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<RouteResponseEntity> updateRouteStatus(@PathVariable Long id){
		var response = service.updateRouteStatus(id, RouteStatus.PLANNED);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/active")
	public ResponseEntity<List<RouteResponseEntity>> getActiveRoutes(){
		var response = service.getActiveRoutes();
		return ResponseEntity.ok(response);
	}
}
