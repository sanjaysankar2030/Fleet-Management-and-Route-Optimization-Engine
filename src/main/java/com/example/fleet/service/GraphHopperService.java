package com.example.fleet.service;

import com.example.fleet.dto.MatrixRequest;
import com.example.fleet.dto.MatrixResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.example.fleet.entity.WayPoint;

import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
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
			MatrixResponse response = callGraphHopperApi(request);

			// Convert response to double[][] (in km)
			double[][] matrix = response.toKilometersMatrix();

			log.info("GraphHopper API call successful. Matrix size: {}x{}", matrix.length, matrix[0].length);
			return matrix;

		} catch (Exception e) {
			log.error("GraphHopper API failed: {}", e.getMessage());
			throw new RuntimeException("GraphHopper Matrix API call failed: " + e.getMessage(), e);
		}
	}

	public MatrixRequest buildGraphHopperRequest(List<WayPoint> wayPoints) {
		List<double[]> matrix = new ArrayList<>();
		for (WayPoint waypoint : wayPoints) {
			double[] location = {waypoint.getLon(), waypoint.getLat()};
			matrix.add(location);
		}
		MatrixRequest request = new MatrixRequest(matrix);
		request.setMode("car");  // Options: car, bike, foot, etc.
		// Why cant it work without setting the mdoe and debug mode
		request.setDebug(false);
		return request;
	}

	public MatrixResponse callGraphHopperApi(MatrixRequest request) throws Exception {
		try {
			String url = graphHopperBaseUrl + "?key=" + graphHopperApiKey;

			// Set headers
			HttpHeaders header = new HttpHeaders();
			header.setContentType(MediaType.APPLICATION_JSON);

			// Set body
			String requestBody = objectMapper.writeValueAsString(request);
			log.debug("GraphHopper request body: {}", requestBody);

			// Post Request to the url
			HttpEntity<String> httpRequest = new HttpEntity<>(requestBody, header);
			MatrixResponse response = restTemplate.postForObject(
					url,
					httpRequest,
					MatrixResponse.class
			);
			return response;
		} catch (Exception e) {
			log.error("Error in callGraphHopperApi()");
			log.error("Will eventually fall back to Haversine ");
			throw e;
		}
	}

//	TODO: Learn what is a jagged matrix is .
	public void validateMatrixResponse(MatrixResponse response, int expectedSize) {
		if (response.getDistances() == null || response.getDistances().isEmpty()) {
			throw new RuntimeException("GraphHopper response has no distance data");
		}

		int actualSize = response.getDistances().size();
		if (actualSize != expectedSize) {
			throw new RuntimeException(
					String.format("Matrix size mismatch: expected %d rows, got %d",
							expectedSize, actualSize)
			);
		}

		// Verify each row is NxN (not jagged)
		for (int i = 0; i < actualSize; i++) {
			int colCount = response.getDistances().get(i).size();
			if (colCount != expectedSize) {
				throw new RuntimeException(
						String.format("Jagged matrix: row %d has %d cols, expected %d",
								i, colCount, expectedSize)
				);
			}
		}
	}
	}
