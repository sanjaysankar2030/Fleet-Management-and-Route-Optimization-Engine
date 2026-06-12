package com.example.fleet.service;

import com.example.fleet.dto.VehicleRequestDTO;
import com.example.fleet.dto.VehicleResponseDTO;
import com.example.fleet.exception.ResourceNotFoundException;
import com.example.fleet.exception.ValidationException;
import com.example.fleet.entity.Vehicle;
import com.example.fleet.entity.enums.VehicleStatus;
import com.example.fleet.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VehicleService {
    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public VehicleResponseDTO registerVehicle(VehicleRequestDTO request) {
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

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAllAvailableVehilce() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);
    }

    public void updateVehicleStatus(Vehicle updatedVehicle, VehicleStatus status) {
        if (status == null) {
            throw new ValidationException("Status" + status + "is not a part of VehicleStatus enum");
        }

        Vehicle vehicle = vehicleRepository.findById(updatedVehicle.getId()).orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        vehicle.setStatus(status);
        vehicleRepository.save(vehicle);
    }

}
