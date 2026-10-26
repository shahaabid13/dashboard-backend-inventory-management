package com.inventory.msp.repository;

import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    List<AppUser> findByRole(UserRole role);

    @Query("""
            SELECT u FROM AppUser u
            WHERE u.active = true
              AND (:query = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY u.fullName, u.username
            """)
    List<AppUser> searchActiveMentionableUsers(@Param("query") String query, Pageable pageable);
}
