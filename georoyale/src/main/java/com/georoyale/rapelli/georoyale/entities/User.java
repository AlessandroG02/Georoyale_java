package com.georoyale.rapelli.georoyale.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long id;
    
    @Column(name = "username", length = 25, unique = true, nullable = false)
    private String username;
    
    @Column(name = "password", length = 255, nullable = false)
    private String password;
    
    @Column(name = "id_point")
    private Integer idPoint;
    
    @Transient
    private int totalQuizzes = 0;
    
    @Transient
    private int correctAnswers = 0;
    
    @Transient
    private int bestStreak = 0;
    
    public double getAccuracy() {
        if (totalQuizzes == 0) return 0.0;
        return (double) correctAnswers / totalQuizzes * 100;
    }
}