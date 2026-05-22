package com.example.fleet.dto;

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
    }
}
