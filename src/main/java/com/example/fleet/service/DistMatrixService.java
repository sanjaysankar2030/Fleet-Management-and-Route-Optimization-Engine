package com.example.fleet.service;

import java.util.List;

import com.example.fleet.entity.WayPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class DistMatrixService {
	@Autowired
	private static final Logger log = LoggerFactory.getLogger(DistMatrixService.class);

	/*
	 * Formula for converting radian / vector to degree ( pi to -pi)
	 * 𝑎=sin2Δlat2+cos(lat1)⋅cos(lat2)⋅sin2Δlong2
	 * atan2 used to find the degree between two points in a 2d plot
	 * c = 2 * atan2(sqrt(a), sqrt(1 - a))
	 * R is the Earth's km
	 * Distance = R ⋅ c
	 * What we are doing here is taking two coordinates (lat, long) and finding the distance between then ( distance of
	 * how the crow flies).
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

	//	Haversine algorithm builds a nxn distance matrix with the List<Waypoints> that
	//	we fetched from the Waypoint entity ( ex : {1,2,3})
	// 	and then builds a matrix out of it
	//	Ex :
	//		 | From/To | W  | A  | B  | C  | D | E  |
	//			| ---- | -- | -- | -- | -- | - | -- |
	//			| W   | 0  | 7  | 4  | 12 | 8 | 15 |
	//			| A   | 7  | 0  | 3  | 9  | 5 | 10 |
	//			| B   | 4  | 3  | 0  | 8  | 6 | 11 |
	//			| C   | 12 | 9  | 8  | 0  | 4 | 2  |
	//			| D   | 8  | 5  | 6  | 4  | 0 | 3  |
	//			| E   | 15 | 10 | 11 | 2  | 3 | 0  |
	//	and then find the smallest distance using greedy Travelling Salesman technique
	//	Ex:
	//	W → B → A → D → E → C

	public double[][] buildHaversineMatrix(List<WayPoint> wayPoints) {
		int n = wayPoints.size();
		double[][] matrix = new double[n][n];
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
			return handleApiException(waypoints);
		}
	}

	public double[][] callDistanceApi(List<WayPoint> waypoints) throws Exception {
		throw new Exception("API NOT IMPLEMENTED STILL FALLING BACK TO HAVERSINE");
	}

	public double[][] handleApiException(List<WayPoint> wayPoints) {
		log.info("--------------------------------------------------------------");
		log.info("External distance API failed. Falling back to Haversine. Reason");
		log.info("--------------------------------------------------------------");
		return buildHaversineMatrix(wayPoints);
	}
}

