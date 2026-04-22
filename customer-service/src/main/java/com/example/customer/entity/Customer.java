package com.example.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
@ToString
@Getter
@Setter
@NoArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(unique = true)
    private String email;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
