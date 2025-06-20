package com.georoyale.rapelli.georoyale.services;

import com.georoyale.rapelli.georoyale.entities.User;

public interface UserService {
    
    User createUser(String username, String password);
    User findByUsername(String username);
    boolean usernameExists(String username);
    User updateUserStats(String username, int totalQuizzes, int correctAnswers, int bestStreak);
}