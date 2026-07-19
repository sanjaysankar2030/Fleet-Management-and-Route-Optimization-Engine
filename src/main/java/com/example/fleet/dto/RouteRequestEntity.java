package com.example.fleet.dto;

import com.example.fleet.entity.Driver;
import com.example.fleet.entity.Route;
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
	private Long VehicleId;
	private Long DriverId;
	private Driver driver;
	private Vehicle vehicle;

	public RouteRequestEntity(Long id , LocalDate date , RouteStatus status,Long Vehicle, Long DriverId){
		this.id = id;
		this.date = date;
		this.status = status;
		this.VehicleId = VehicleId;
		this.DriverId = DriverId;
	}
}
