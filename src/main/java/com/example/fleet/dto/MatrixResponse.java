package com.example.fleet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatrixResponse {
	@JsonProperty("distances")
	public List<List<Long>> distances;

	@JsonProperty("times")
	public List<List<Long>> times;

	@JsonProperty("info")
	public InfoObject info;

	// Convert meters to km
	public double[][] toKilometersMatrix() {
		if (distances == null || distances.isEmpty()) {
			throw new RuntimeException("GraphHopper response has no distances");
		}

		int n = distances.size();
		double[][] matrixKm = new double[n][n];

		for (int i = 0; i < n; i++) {
			List<Long> row = distances.get(i);
			for (int j = 0; j < row.size(); j++) {
				// Convert meters to kilometers
				matrixKm[i][j] = row.get(j) / 1000.0;
			}
		}

		return matrixKm;
	}

	@Data
	public static class InfoObject{
		List<String> copyrights;
		List<String> hints;
	}
}

