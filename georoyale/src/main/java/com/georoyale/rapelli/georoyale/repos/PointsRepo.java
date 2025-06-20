package com.georoyale.rapelli.georoyale.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.georoyale.rapelli.georoyale.entities.Points;
import com.georoyale.rapelli.georoyale.entities.User;
import java.util.List;

public interface PointsRepo extends JpaRepository<Points, Long> {
    
    // Trova tutti i punteggi di un utente ordinati per data (più recenti prima)
    List<Points> findByUserOrderByDataPointDesc(User user);
    
    // Trova il punteggio più alto di un utente
    @Query("SELECT MAX(p.totPoint) FROM Points p WHERE p.user = :user")
    Integer findMaxPointsByUser(@Param("user") User user);
    
    // Trova gli ultimi N punteggi di un utente
    @Query("SELECT p FROM Points p WHERE p.user = :user ORDER BY p.dataPoint DESC")
    List<Points> findLastPointsByUser(@Param("user") User user);
    
    // Conta il numero totale di quiz fatti da un utente
    @Query("SELECT COUNT(p) FROM Points p WHERE p.user = :user")
    Long countQuizzesByUser(@Param("user") User user);
    
    // Somma totale punti di un utente
    @Query("SELECT COALESCE(SUM(p.totPoint), 0) FROM Points p WHERE p.user = :user")
    Integer sumPointsByUser(@Param("user") User user);
}