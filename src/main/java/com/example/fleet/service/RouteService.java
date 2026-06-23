package com.example.fleet.service;

import com.example.fleet.dto.RouteResponseEntity;
import com.example.fleet.dto.RouteRequestEntity;
import com.example.fleet.entity.Route;
import com.example.fleet.entity.enums.RouteStatus;
import com.example.fleet.exception.ResourceNotFoundException;
import com.example.fleet.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RouteService {
	@Autowired
	private RouteRepository repository;

	public RouteResponseEntity createRoute(RouteRequestEntity request) {
		Route route = new Route(
				request.getId(),
				request.getDate(),
				request.getStatus(),
				request.getVehicle(),
				request.getDriver()
		);
		repository.save(route);
		return new RouteResponseEntity(
				request.getId(),
				request.getDate(),
				request.getStatus(),
				request.getVehicle(),
				request.getDriver()
		);
	}

	public RouteResponseEntity getRouteById(Long id) {
		Route route = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Route Does not exist"));
		return new RouteResponseEntity(
				route.getId(),
				route.getDate(),
				route.getStatus(),
				route.getVehicle(),
				route.getDriver()
		);
	}

	public List<RouteResponseEntity> getAllRoutes() {
		return repository.findAll().stream().map(
				response -> new RouteResponseEntity(
						response.getId(),
						response.getDate(),
						response.getStatus(),
						response.getVehicle(),
						response.getDriver()
				)
		).toList();
	}

	public List<RouteResponseEntity> getRouteByVehicle(Long vehicleId) {
		List<Route> vehilceRoute = new ArrayList<>();
		List<Route> base = repository.findAll();
		for (Route route : base) {
			if (route.getVehicle().getId().equals(vehicleId)) {
				vehilceRoute.add(route);
			}
		}
		return vehilceRoute.stream()
				.map(
						response -> new RouteResponseEntity(
								response.getId(),
								response.getDate(),
								response.getStatus(),
								response.getVehicle(),
								response.getDriver()
						)).toList();
	}

	public List<RouteResponseEntity> getTasksByDriver(Long driverId) {
		List<Route> driverList = new ArrayList<>();
		List<Route> base = repository.findAll();
		for (Route deliverytask : base) {
			if (deliverytask.getDriver().getId().equals(driverId)) {
				driverList.add(deliverytask);
			}
		}
		return driverList.stream()
				.map(
						response -> new RouteResponseEntity(
								response.getId(),
								response.getDate(),
								response.getStatus(),
								response.getVehicle(),
								response.getDriver()
						)).toList();
	}

	public RouteResponseEntity updateRouteStatus(Long id, RouteStatus newStatus) {
		Route response = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Route not found "));
		response.setStatus(newStatus);
		repository.save(response);
		return new RouteResponseEntity(
				response.getId(),
				response.getDate(),
				response.getStatus(),
				response.getVehicle(),
				response.getDriver()
		);
	}

	public List<RouteResponseEntity> getActiveRoutes() {
		var responseList = getAllRoutes();
		List<RouteResponseEntity> activeList = new ArrayList<>();

		return responseList.stream()
				.filter(response -> response.getStatus() == RouteStatus.ACTIVE)
				.map(response -> new RouteResponseEntity(
						response.getId(),
						response.getDate(),
						response.getStatus(),
						response.getVehicle(),
						response.getDriver()
				)).collect(Collectors.toList());
	}

}