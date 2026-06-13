package com.example.fleet.service;

import com.example.fleet.exception.ResourceNotFoundException;
import com.example.fleet.exception.ValidationException;
import com.example.fleet.dto.DriverRequestDTO;
import com.example.fleet.dto.DriverResponseDTO;
import com.example.fleet.entity.Driver;
import com.example.fleet.entity.enums.DriverStatus;
import com.example.fleet.repository.DriverRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


@Service
public class DriverService {
    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public DriverResponseDTO registerDriver(@NonNull DriverRequestDTO request) {
        if (request.getLicenseNumber() == null || request.getLicenseNumber().isBlank()) {
            throw new ValidationException("License number cannot be empty");
        }

        Driver driver = Driver.builder()
                .name(request.getName())
                .licenseNumber(request.getLicenseNumber())
                .licenseValidUntil(request.getLicenseValidUntil())
                .shiftHours(request.getShiftHours())
                .status(DriverStatus.AVAILABLE)
                .build();

        Driver saved = driverRepository.save(driver);

        return new DriverResponseDTO(saved.getId(), saved.getName(), saved.getLicenseNumber(),
                saved.getLicenseValidUntil(), saved.getShiftHours(), saved.getStatus());
    }

    public DriverResponseDTO getDriverById(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        return new DriverResponseDTO(
                driver.getId(),
                driver.getName(),
                driver.getLicenseNumber(),
                driver.getLicenseValidUntil(),
                driver.getShiftHours(),
                driver.getStatus()
        );

    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public void updateDriverShiftHours(Long driverId, double newShiftHours) {

        if (newShiftHours <= 0) {
            throw new ValidationException("Shift hours must be greater than 0");
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        driver.setShiftHours(newShiftHours);

        driverRepository.save(driver);
    }

    public boolean validateDriverLicense(@NonNull Driver driver) {
        return LocalDate.now().isBefore(driver.getLicenseValidUntil());
    }

    public boolean isAvailable(Driver driver) {
        return driverRepository.findByStatus(DriverStatus.AVAILABLE);
    }

}


