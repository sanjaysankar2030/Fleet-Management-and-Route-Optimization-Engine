package com.example.fleet.service;

import com.example.fleet.dto.MatrixRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.example.fleet.entity.WayPoint;

import java.util.List;

@Service
public class GraphHopperService {
	private static final Logger log = LoggerFactory.getLogger(GraphHopperService.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Value("${graphhopper.api.key}")
	private String graphHopperApiKey;

	@Value("${graphhopper.api.base-url}")
	private String graphHopperBaseUrl;

	@Value("${graphhopper.api.timeout-seconds:30}")
	private int timeoutSeconds;

	@Value("${graphhopper.api.enable:true}")

	private boolean graphHopperEnabled;

	public double[][] getDistanceMatrixFromGraphHopper(List<WayPoint> waypoints) throws Exception {
		if (!graphHopperEnabled) {
			throw new RuntimeException("GraphHopper API is disabled in configuration");
		}

		try {
			log.info("Calling GraphHopper Matrix API for {} waypoints", waypoints.size());

			// Convert waypoints to GraphHopper format
			MatrixRequest request = buildGraphHopperRequest(waypoints);

			// Make API call
			GraphHopperMatrixResponse response = callGraphHopperApi(request);

			// Convert response to double[][] (in km)
			double[][] matrix = response.toKilometersMatrix();

			log.info("GraphHopper API call successful. Matrix size: {}x{}", matrix.length, matrix[0].length);
			return matrix;

		} catch (Exception e) {
			log.error("GraphHopper API failed: {}", e.getMessage());
			throw new RuntimeException("GraphHopper Matrix API call failed: " + e.getMessage(), e);
		}
	}
}
