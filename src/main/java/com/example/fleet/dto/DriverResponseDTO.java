package com.example.fleet.dto;

import com.example.fleet.entity.enums.DriverStatus;
import com.example.fleet.entity.enums.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponseDTO {
    private Long id;
    private String name;
    private String licenseNumber;
    private LocalDate licenseValidUntil;
    private double shiftHours;
    private DriverStatus status;
}