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
        try {
            if (usernameExists(username)) {
                throw new RuntimeException("Username già esistente: " + username);
            }
            
            if (username == null || username.trim().isEmpty()) {
                throw new RuntimeException("Username non può essere vuoto");
            }
            
            if (password == null || password.length() < 6) {
                throw new RuntimeException("Password deve essere almeno 6 caratteri");
            }
            
            User user = new User();
            user.setUsername(username.trim());
            user.setPassword(passwordEncoder.encode(password));
            user.setIdPoint(0);
            
            return userRepo.save(user);
            
        } catch (Exception e) {
            throw new RuntimeException("Errore nella registrazione: " + e.getMessage());
        }
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