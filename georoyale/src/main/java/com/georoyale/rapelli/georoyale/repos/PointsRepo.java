package com.georoyale.rapelli.georoyale.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.georoyale.rapelli.georoyale.entities.Points;
import com.georoyale.rapelli.georoyale.entities.User;
import java.util.List;

public interface PointsRepo extends JpaRepository<Points, Long> {
    
    List<Points> findByUserOrderByDataPointDesc(User user);
    
    @Query("SELECT MAX(p.totPoint) FROM Points p WHERE p.user = :user")
    Integer findMaxPointsByUser(@Param("user") User user);
    
    @Query("SELECT p FROM Points p WHERE p.user = :user ORDER BY p.dataPoint DESC")
    List<Points> findLastPointsByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(p) FROM Points p WHERE p.user = :user")
    Long countQuizzesByUser(@Param("user") User user);
    
    @Query("SELECT COALESCE(SUM(p.totPoint), 0) FROM Points p WHERE p.user = :user")
    Integer sumPointsByUser(@Param("user") User user);
}