package com.example.scheduler.Repository;

import com.example.scheduler.Model.MasterTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterTaskRepository extends JpaRepository<MasterTask, String> {
    MasterTask findTopById(String id);
}
