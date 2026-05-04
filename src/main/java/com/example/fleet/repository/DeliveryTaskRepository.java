package com.example.fleet.repository;

import com.example.fleet.entity.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask,Long> {

}
