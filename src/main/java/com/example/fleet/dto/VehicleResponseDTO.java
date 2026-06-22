package com.example.fleet.dto;

import com.example.fleet.entity.Vehicle;
import com.example.fleet.entity.enums.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class VehicleResponseDTO {

    private Long id;

    private String licensePlate ;

    private VehicleStatus status;

    private double capacity;

    private LocalDate lastMaintenanceDate ;

    public VehicleResponseDTO(Long id, String licensePlate, double capacity, VehicleStatus status) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.capacity = capacity;
        this.status = status;
    }

    public VehicleResponseDTO(Vehicle vehicle) {
        this.id = vehicle.getId();
        this.licensePlate = vehicle.getLicensePlate();
        this.capacity = vehicle.getCapacity();
        this.status = vehicle.getStatus();
        this.lastMaintenanceDate = vehicle.getLastMaintenanceDate();
    }
}
