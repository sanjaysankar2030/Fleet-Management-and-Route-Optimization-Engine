package com.example.fleet.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverRequestDTO {
    private String name;
    private String licenseNumber;
    private LocalDate licenseValidUntil;
    private double shiftHours;
}