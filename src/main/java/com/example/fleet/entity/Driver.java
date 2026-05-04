package com.example.fleet.entity;

import com.example.fleet.entity.enums.VehicleStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import lombok.Data;


@Data
@Entity
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String licensePlate;
    private double capacity;

    // The VehicleStatus is implemented here
    @Enumerated(EnumType.STRING)
    private VehicleStatus status;

    private LocalDate lastMaintenanceDate;

}
