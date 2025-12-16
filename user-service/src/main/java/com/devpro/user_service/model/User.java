package com.devpro.user_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {
    @Id
    private String id;

    @Column
    private String username;

    @Column(unique = true, nullable = false)
    private String email;
    
    // Getters and Setters

}
