package com.example.fleet.service;

import com.example.fleet.dto.DriverResponseDTO;
import com.example.fleet.repository.VehicleRepository;
import com.example.fleet.dto.VehicleRequestDTO;
import com.example.fleet.dto.VehicleResponseDTO;
import com.example.fleet.entity.Driver;
import com.example.fleet.entity.enums.DriverStatus;
import com.example.fleet.exception.ResourceNotFoundException;
import com.example.fleet.exception.ValidationException;
import com.example.fleet.entity.Vehicle;
import com.example.fleet.entity.enums.VehicleStatus;
import com.example.fleet.repository.DriverRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleService(VehicleRepository vehicleRepository, DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    public VehicleResponseDTO registerVehicle(@NonNull VehicleRequestDTO request) {
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setCapacity(request.getCapacity());
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setLastMaintenanceDate(request.getLastMaintenanceDate());
        Vehicle saved = vehicleRepository.save(vehicle);
        return new VehicleResponseDTO(saved.getId(), saved.getLicensePlate(), saved.getCapacity(), saved.getStatus());
    }

    public VehicleResponseDTO getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));

        return new VehicleResponseDTO(vehicle.getId(), vehicle.getLicensePlate(), vehicle.getStatus(), vehicle.getCapacity(), vehicle.getLastMaintenanceDate());
    }

    public List<VehicleResponseDTO> getAllVehicles() {
        return vehicleRepository.findAll().
                stream()
                .map(vehicles -> new VehicleResponseDTO(
                vehicles.getId(),
                vehicles.getLicensePlate(),
                vehicles.getStatus(),
                vehicles.getCapacity(),
                vehicles.getLastMaintenanceDate()
        ))
                .toList();
    }


    public List<VehicleResponseDTO> getAllAvailableVehilce() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)
        .stream()
                .map(vehicles -> new VehicleResponseDTO(
                        vehicles.getId(),
                        vehicles.getLicensePlate(),
                        vehicles.getStatus(),
                        vehicles.getCapacity(),
                        vehicles.getLastMaintenanceDate()
                ))
                .toList();
    }

    public void updateVehicleStatus(Long id, VehicleStatus status) {
        if (status == null) {
            throw new ValidationException("Status" + status + "is not a part of VehicleStatus enum");
        }

        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        vehicle.setStatus(status);
        vehicleRepository.save(vehicle);
    }

    public Vehicle assignDriverToVehicle(Long vehicleId, Long driverId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        // Guard checks
        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new IllegalStateException("Cannot assign a vehicle that is under maintenance");
        }

        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new IllegalStateException("Driver is not available for assignment");
        }

        // Assign and update statuses
        vehicle.setDriver(driver);
        vehicle.setStatus(VehicleStatus.IN_USE);
        driver.setStatus(DriverStatus.ON_SHIFT);  // or whatever your enum value is

        return vehicleRepository.save(vehicle);
    }

    public void updateMaintenanceDate(Long vehicle_id, LocalDate newDate) {
        //private LocalDate lastMaintenanceDate ;
        if (newDate == null) {
            throw new ValidationException("Mantaince Date is null , It Cannot be Null");
        }
        Vehicle vh = vehicleRepository.findById(vehicle_id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle Not Found"));
        vh.setLastMaintenanceDate(newDate);
        vehicleRepository.save(vh);

    }

    private void validateVehicleCapacity(double capacity) {
        if (capacity <= 0) {
            throw new ValidationException("Capacity must be greater than 0");
        }
        if (capacity > 50000) {  // 50 tons in kg — max for a heavy truck
            throw new ValidationException("Capacity exceeds maximum allowed limit");
        }
    }
}
