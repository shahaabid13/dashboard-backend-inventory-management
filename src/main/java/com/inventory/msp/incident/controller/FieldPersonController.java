package com.inventory.msp.incident.controller;

import com.inventory.msp.incident.dto.FieldPersonRequest;
import com.inventory.msp.incident.dto.FieldPersonResponse;
import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.service.IncidentMapper;
import com.inventory.msp.incident.service.FieldPersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incidents/field-persons")
@RequiredArgsConstructor
public class FieldPersonController {

    private final FieldPersonService fieldPersonService;
    private final IncidentMapper incidentMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FieldPersonResponse> createFieldPerson(
            @Valid @RequestBody FieldPersonRequest request) {
        FieldPerson fieldPerson = fieldPersonService.createFieldPerson(
                request.getName(),
                request.getRole(),
                request.getPhone(),
                request.getUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentMapper.toFieldPersonResponse(fieldPerson));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_ENGINEER', 'FIELD_PERSON', 'REVIEWER')")
    public ResponseEntity<FieldPersonResponse> getFieldPerson(@PathVariable Long id) {
        FieldPerson fieldPerson = fieldPersonService.getFieldPerson(id);
        return ResponseEntity.ok(incidentMapper.toFieldPersonResponse(fieldPerson));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_ENGINEER', 'FIELD_PERSON', 'REVIEWER')")
    public ResponseEntity<List<FieldPersonResponse>> getAllFieldPersons() {
        List<FieldPerson> fieldPersons = fieldPersonService.getAllFieldPersons();
        return ResponseEntity.ok(fieldPersons.stream()
                .map(incidentMapper::toFieldPersonResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_ENGINEER', 'FIELD_PERSON', 'REVIEWER')")
    public ResponseEntity<List<FieldPersonResponse>> getActiveFieldPersons() {
        List<FieldPerson> fieldPersons = fieldPersonService.getActiveFieldPersons();
        return ResponseEntity.ok(fieldPersons.stream()
                .map(incidentMapper::toFieldPersonResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/assignable")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_ENGINEER', 'FIELD_PERSON')")
    public ResponseEntity<List<FieldPersonResponse>> getAssignableFieldPersons() {
        List<FieldPerson> fieldPersons = fieldPersonService.getAssignableFieldPersons();
        return ResponseEntity.ok(fieldPersons.stream()
                .map(incidentMapper::toFieldPersonResponse)
                .collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FieldPersonResponse> updateFieldPerson(
            @PathVariable Long id,
            @Valid @RequestBody FieldPersonRequest request) {
        FieldPerson fieldPerson = fieldPersonService.updateFieldPerson(
                id,
                request.getName(),
                request.getRole(),
                request.getPhone(),
                request.getActive(),
                request.getUserId()
        );
        return ResponseEntity.ok(incidentMapper.toFieldPersonResponse(fieldPerson));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFieldPerson(@PathVariable Long id) {
        fieldPersonService.deleteFieldPerson(id);
        return ResponseEntity.noContent().build();
    }
}