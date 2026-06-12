package com.example.fleet.repository;

import com.example.fleet.entity.enums.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fleet.entity.Driver;

public interface DriverRepository extends JpaRepository<Driver,Long> {

    boolean findByStatus(DriverStatus driverStatus);
}
