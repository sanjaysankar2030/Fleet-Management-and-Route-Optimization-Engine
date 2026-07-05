package com.example.fleet.service;

import com.example.fleet.entity.WayPoint;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.logging.Logger;

@Service
public class DistMatrixService {
	@Autowired
	private static final Logger log = LoggerFactory.getLogger(DistMatrixService.class);

	@Autowired
	public DeliveryTaskService deliveryTaskService;

	@Autowired
	public RestTemplate restTemplate;

//    @Value("${distance.api.google-api-key}")
//    private String googleApiKey;
//
//	@Value("${distance.api.google-api-key}")
//	private String googleBaseUrl;

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
	public double[][] buildHaversineMatrix(List<WayPoint> wayPoints){
		int n = wayPoints.size();
		double [][] matrix = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (i == j) {
					matrix[i][j] = 0;
				} else {
					matrix[i][j] = calcDistBetweenCoordinates(
							wayPoints.get(i).getLat(), wayPoints.get(i).getLon(),
							wayPoints.get(j).getLat(), wayPoints.get(j).getLon()
					);
				}
			}
		}
		return matrix;
	}
	public double[][] getDistanceMatrix(List<WayPoint> waypoints) {
		try {
			return callDistanceApi(waypoints);
		} catch (Exception e) {
			return handleApiException(e, waypoints);
		}
	}

	public Exception callDistanceApi(List<WayPoint> waypoints) {
		return new Exception("API NOT IMPMLEMENTED STILL FALLING BACK TO HAVERSINE");
	}

	public double[][] handleApiException(Exception e, List<WayPoint> wayPoints) {
		log.warning("--------------------------------------------------------------");
		log.warning("External distance API failed. Falling back to Haversine. Reason");
		log.warning("--------------------------------------------------------------");
		return buildHaversineMatrix(wayPoints);
	}
}

