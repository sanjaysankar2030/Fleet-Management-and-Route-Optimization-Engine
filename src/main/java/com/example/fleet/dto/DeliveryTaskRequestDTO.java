package com.example.fleet.dto;

import com.example.fleet.entity.Driver;
import com.example.fleet.entity.Vehicle;
import com.example.fleet.entity.enums.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryTaskRequestDTO {
	private Long id;

	private String address;
	private double latitude;
	private double longitude;
	private DeliveryStatus status;

	private Vehicle vehicle;

	private Driver driver;
}

