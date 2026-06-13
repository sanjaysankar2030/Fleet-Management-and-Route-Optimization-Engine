package com.example.fleet.entity;

import com.example.fleet.entity.enums.DriverStatus;
import jakarta.persistence.*;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Driver {
//    id | name | licenseNumber | licenseValidUntil | shiftHours | status
//    Hello
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String licenseNumber;

    // The VehicleStatus is implemented here
    @Enumerated(EnumType.STRING)
    private DriverStatus status;

    private LocalDate licenseValidUntil;
    private double shiftHours;
}
