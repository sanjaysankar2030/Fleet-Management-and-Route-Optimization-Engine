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
public class VehicleRequestDTO {
    private String licensePlate ;
    private double capacity;
    private LocalDate lastMaintenanceDate ;

}
