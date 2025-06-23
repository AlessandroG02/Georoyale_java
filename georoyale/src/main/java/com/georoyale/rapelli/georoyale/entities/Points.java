package com.georoyale.rapelli.georoyale.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "points")
public class Points {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_point")
    private Long idPoint;
    
    @Column(name = "data_point")
    private LocalDateTime dataPoint;
    
    @Column(name = "tot_point")
    private Integer totPoint;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    public Points(User user, Integer totPoint) {
        this.user = user;
        this.totPoint = totPoint;
        this.dataPoint = LocalDateTime.now();
    }
}