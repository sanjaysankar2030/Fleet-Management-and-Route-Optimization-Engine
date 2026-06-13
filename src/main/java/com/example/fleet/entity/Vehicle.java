    package com.example.fleet.entity;

    import com.example.fleet.entity.enums.VehicleStatus;
    import jakarta.persistence.*;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.time.LocalDate;

    @Data
    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    public class Vehicle {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    //    private Long driverID;
        @Column(unique = true, nullable = false)
        private String licensePlate ;

        @Enumerated(EnumType.STRING)
        private VehicleStatus status;

        private double capacity;

        private LocalDate lastMaintenanceDate ;

        @ManyToOne
        @JoinColumn(name ="driver_id")
        private Driver driver;

    }
