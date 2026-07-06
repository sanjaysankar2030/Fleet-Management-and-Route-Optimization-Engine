package com.example.fleet.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispatchRequestDTO {
	private Long id;
	private Long driverId;
	private Long vehicleId;
	private List<Long> deliveryListIds;

}


