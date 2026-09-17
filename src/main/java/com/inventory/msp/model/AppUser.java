package com.inventory.msp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_user")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private String password;          // encrypted

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "agency_name")
    private String agencyName;        // only for agency accounts

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone; // E.164 or local format depending on data
}