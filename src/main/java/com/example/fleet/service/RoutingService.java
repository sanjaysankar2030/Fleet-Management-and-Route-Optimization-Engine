package com.example.fleet.service;

import com.example.fleet.entity.DeliveryTask;
import com.example.fleet.entity.WayPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingService {
	@Autowired
	private DistMatrixService distMatrixService;


	public List<DeliveryTask> optimizeRoute(List<DeliveryTask> deliveryTasks) {
		List<WayPoint> wayPoints = buildWaypointMatrix(deliveryTasks);
		double[][] matrix = distMatrixService.getDistanceMatrix(wayPoints);
		List<Integer> bestRoute = applyGreedyTSP(matrix);
		return sequencePoints(bestRoute, deliveryTasks);
	}

	public List<WayPoint> buildWaypointMatrix(List<DeliveryTask> deliveryTaskList) {
		List<WayPoint> wayPointList = new ArrayList<>();
		for (DeliveryTask deliveryTask : deliveryTaskList) {
			wayPointList.add(new WayPoint(deliveryTask.getId(), deliveryTask.getLatitude(), deliveryTask.getLongitude()));
		}
		return wayPointList;
	}

	public List<Integer> applyGreedyTSP(double[][] distanceMatrix) {
		int n = distanceMatrix.length;
		boolean[] visited = new boolean[n];
		List<Integer> route = new ArrayList<>();

		int current = 0; // start at index 0 (depot or first stop)
		visited[current] = true;
		route.add(current);

		for (int step = 1; step < n; step++) {
			int nearest = -1;
			double minDist = Double.MAX_VALUE;

			for (int j = 0; j < n; j++) {
				if (!visited[j] && distanceMatrix[current][j] < minDist) {
					minDist = distanceMatrix[current][j];
					nearest = j;
				}
			}

			visited[nearest] = true;
			route.add(nearest);
			current = nearest;
		}

		return route;
	}
	public List<DeliveryTask> sequencePoints(List<Integer> bestRoute, List<DeliveryTask> deliveryTasks) {
		List<DeliveryTask> ordered = new ArrayList<>();
		for (int index : bestRoute) {
			ordered.add(deliveryTasks.get(index));
		}
		return ordered;
	}

	public double calculateTotalDistance(List<Integer> sequence, double[][] distanceMatrix) {
		double total = 0;
		for (int i = 0; i < sequence.size() - 1; i++) {
			total += distanceMatrix[sequence.get(i)][sequence.get(i + 1)];
		}
		return total;
	}
}
