package com.inventory.msp.repository;

import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
=======
>>>>>>> 9ef025098d86db3f85cd377679ee6b39756e7de6

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    List<AppUser> findByRole(UserRole role);
<<<<<<< HEAD

    @Query("""
            SELECT u FROM AppUser u
            WHERE u.active = true
              AND (:query = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY u.fullName, u.username
            """)
    List<AppUser> searchActiveMentionableUsers(@Param("query") String query, Pageable pageable);
=======
>>>>>>> 9ef025098d86db3f85cd377679ee6b39756e7de6
}
