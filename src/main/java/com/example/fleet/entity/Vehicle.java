package com.example.fleet.entity;

import com.example.fleet.entity.enums.VehicleStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String licensePlate ;

    @Enumerated(EnumType.STRING)
    private VehicleStatus status;

    private double capacity;

    private LocalDate lastMaintenanceDate ;

}
