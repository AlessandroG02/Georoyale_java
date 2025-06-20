package com.georoyale.rapelli.georoyale.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.georoyale.rapelli.georoyale.entities.User;
import com.georoyale.rapelli.georoyale.repos.UserRepo;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
public User createUser(String username, String password) {
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setIdPoint(1); // Usa l'id_point esistente nella tabella points
    
    return userRepo.save(user);
}

    @Override
    public User findByUsername(String username) {
        return userRepo.findByUsername(username).orElse(null);
    }

    @Override
    public boolean usernameExists(String username) {
        return userRepo.existsByUsername(username);
    }

    @Override
    public User updateUserStats(String username, int totalQuizzes, int correctAnswers, int bestStreak) {
        User user = findByUsername(username);
        if (user != null) {
            user.setTotalQuizzes(totalQuizzes);
            user.setCorrectAnswers(correctAnswers);
            user.setBestStreak(bestStreak);
            return userRepo.save(user);
        }
        return null;
    }
}