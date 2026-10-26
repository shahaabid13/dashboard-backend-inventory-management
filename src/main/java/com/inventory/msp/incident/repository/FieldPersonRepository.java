package com.inventory.msp.incident.repository;

import com.inventory.msp.incident.model.FieldPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FieldPersonRepository extends JpaRepository<FieldPerson, Long> {

    List<FieldPerson> findByActive(Boolean active);

    Optional<FieldPerson> findByUserId(Long userId);

    List<FieldPerson> findByUserIdIsNotNull();
}

