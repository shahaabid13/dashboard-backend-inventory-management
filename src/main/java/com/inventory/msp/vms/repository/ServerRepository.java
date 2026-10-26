package com.inventory.msp.vms.repository;

import com.inventory.msp.vms.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerRepository extends JpaRepository<Server, Integer> {
    List<Server> findByIsActiveTrue();
}
