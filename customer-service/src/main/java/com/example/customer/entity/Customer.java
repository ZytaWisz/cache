package com.example.customer.entity;

import com.example.customer.listener.AuditListener;
import com.example.customer.model.Creatable;
import com.example.customer.model.Updatable;
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
@EntityListeners(AuditListener.class)
public class Customer implements Creatable, Updatable {
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
    @Setter(AccessLevel.NONE)
    private Long version;

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Customer(String name, String email, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
