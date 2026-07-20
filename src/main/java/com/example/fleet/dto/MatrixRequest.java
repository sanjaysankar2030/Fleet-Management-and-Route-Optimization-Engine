package com.example.fleet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatrixRequest {
	private List<double[]> locations;
	private String mode;
	private boolean debug;

	public MatrixRequest(List<double[]> locations ){
		this.locations = locations;
		this.mode = "car";
		this.debug = false;
	}

}
