package com.example.fleet.dto;

import com.example.fleet.entity.Driver;
import com.example.fleet.entity.Vehicle;
import com.example.fleet.entity.enums.RouteStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteRequestEntity {

	private Long id;
	private LocalDate date;
	private RouteStatus status;
	private Vehicle vehicle;
	private Driver driver;
}
