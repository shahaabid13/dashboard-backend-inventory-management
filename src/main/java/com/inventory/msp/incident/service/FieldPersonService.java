package com.inventory.msp.incident.service;

import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.repository.FieldPersonRepository;
import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FieldPersonService {

    private final FieldPersonRepository fieldPersonRepository;
    private final UserRepository userRepository;

    public FieldPerson createFieldPerson(String name, String role, String phone, Long userId) {
        FieldPerson.FieldPersonBuilder builder = FieldPerson.builder()
                .name(name)
                .role(role)
                .phone(phone)
                .active(true);

        if (userId != null) {
            builder.user(resolveAndValidateUser(userId));
        }

        return fieldPersonRepository.save(builder.build());
    }

    public FieldPerson getFieldPerson(Long id) {
        return fieldPersonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Field person not found with id: " + id));
    }

    public List<FieldPerson> getAllFieldPersons() {
        return fieldPersonRepository.findAll();
    }

    public List<FieldPerson> getActiveFieldPersons() {
        return fieldPersonRepository.findByActive(true);
    }

    public List<FieldPerson> getAssignableFieldPersons() {
        return fieldPersonRepository.findByUserIdIsNotNull();
    }

    public FieldPerson updateFieldPerson(Long id, String name, String role, String phone, Boolean active, Long userId) {
        FieldPerson person = getFieldPerson(id);
        if (name != null) {
            person.setName(name);
        }
        if (role != null) {
            person.setRole(role);
        }
        if (phone != null) {
            person.setPhone(phone);
        }
        if (active != null) {
            person.setActive(active);
        }
        if (userId != null) {
            person.setUser(resolveAndValidateUser(userId));
        }
        return fieldPersonRepository.save(person);
    }

    public void deleteFieldPerson(Long id) {
        FieldPerson person = getFieldPerson(id);
        fieldPersonRepository.delete(person);
    }

    /**
     * Looks up the AppUser by id and confirms it exists and has the FIELD_PERSON role
     * before linking it to a FieldPerson profile.
     */
    private AppUser resolveAndValidateUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        if (user.getRole() != UserRole.FIELD_PERSON) {
            throw new IllegalArgumentException(
                    "User '" + user.getUsername() + "' does not have the FIELD_PERSON role, cannot link.");
        }

        return user;
    }
}