package com.georoyale.rapelli.georoyale.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import com.georoyale.rapelli.georoyale.entities.User;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}