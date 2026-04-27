package com.example.fleet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.fleet.entity.Driver;

public interface DriverRepository extends JpaRepository<Driver,Long> {
}
