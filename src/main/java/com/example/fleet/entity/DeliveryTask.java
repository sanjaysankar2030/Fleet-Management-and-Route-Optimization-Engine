package com.example.fleet.entity;

import com.example.fleet.entity.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class DeliveryTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String address;
    private double latitude;
    private double longitude;
    private DeliveryStatus status;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name="driver_id")
    private Driver driver;
}
