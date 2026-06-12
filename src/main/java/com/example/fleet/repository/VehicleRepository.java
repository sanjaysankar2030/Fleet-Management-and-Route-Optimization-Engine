package com.example.fleet.repository;

import com.example.fleet.entity.Vehicle;
import com.example.fleet.entity.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle,Long> {

    List<Vehicle> findByStatus(VehicleStatus vehicleStatus);
}


