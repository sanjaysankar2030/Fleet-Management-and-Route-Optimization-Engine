package com.example.fleet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptimizedRouteResponseDTO {
	private Long routeId;
	private Long vehicleId;
	private Long driverId;
	private List<OrderedDeliveryStop> stops;
	private double totalDistanceKm;
	private String status;
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OrderedDeliveryStop {
		private int sequence;
		private Long taskId;
		private String address;
		private double latitude;
		private double longitude;
		private double distanceFromPreviousKm;
	}
}

