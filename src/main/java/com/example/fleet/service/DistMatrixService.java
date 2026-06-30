package com.example.fleet.service;

import org.springframework.stereotype.Service;

@Service
public class DistMatrixService {
	public DeliveryTaskService deliveryTaskService;

	/*
	 * Forumla for converting radian / vector to degree ( pi to -pi)
	 * 𝑎=sin2Δlat2+cos(lat1)⋅cos(lat2)⋅sin2Δlong2
	 * atan2 used to find the degree between two points in a 2d plot
	 * c = 2 * atan2(sqrt(a), sqrt(1 - a))
	 * R is the Earth's km
	 * Distance = R ⋅ c
	 */
	public double calcDistBetweenCoordinates(double lat1, double long1, double lat2, double long2) {
		double R = 6371;
		double latRad = Math.toRadians(lat2 - lat1);
		double lonRad = Math.toRadians(long2 - long1);
		double a = Math.sin(latRad / 2) * Math.sin(latRad / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(lonRad / 2) * Math.sin(lonRad / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return R * c;
	}
	//Take lat/lng from each DeliveryTaskEntity → buildWaypointMatrix converts them into a List<Waypoint> (depot first).
	//buildMatrix / getDistanceMatrix loops over every pair of waypoints and calls calculateDistanceBetweenPoints (Haversine) for each → produces the NxN matrix of distances in km.
	//applyGreedyTSP walks that matrix starting from the depot, always jumping to the nearest unvisited stop, until all stops are visited → produces an order (sequence of indices), not just a number.
	//calculateTotalDistance adds up the matrix values along that sequence → gives you the total km for that route.
	//sequenceWaypoints maps the index order back to the real DeliveryTaskEntity objects.
	// Sequence + total distance get packed into OptimizedRouteResponseDTO (and saved to RouteEntity if persisted).	// and also why are there list of waypoints ?
}
