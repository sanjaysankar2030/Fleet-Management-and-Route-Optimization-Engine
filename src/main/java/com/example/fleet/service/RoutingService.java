package com.example.fleet.service;

import com.example.fleet.entity.DeliveryTask;
import com.example.fleet.entity.WayPoint;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class RoutingService {
	@Autowired
	private DistMatrixService distMatrixService;

	public List<DeliveryTask> optimizeRoutes(List<DeliveryTask> deliveryTasks) {
		List<WayPoint> wayPoints = buildWaypointMatrix(deliveryTasks);
		double[][] matrix = distMatrixService.getDistanceMatrix(wayPoints);
		List<Integer> bestRoute = applyTSP(wayPoints);
		return sequencePoints(bestRoute, deliveryTasks);
	}

	public List<WayPoint> buildWaypointMatrix(List<DeliveryTask> deliveryTaskList) {
		List<WayPoint> wayPointList = new ArrayList<>();
		for (DeliveryTask deliveryTask : deliveryTaskList) {
			wayPointList.add(new WayPoint(deliveryTask.getId(), deliveryTask.getLatitude(), deliveryTask.getLongitude()));
		}
		return wayPointList;
	}

	public List<Integer> applyTSP(List<WayPoint> wayPointList) {
		List<Integer> bestRouteList = new ArrayList<>();
		return bestRouteList;
//		TODO: Learn the greedy tsp with golang and then try to implement it here
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
