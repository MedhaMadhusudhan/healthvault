package com.healthvault.ehv.service;

import com.healthvault.ehv.model.User;
import com.healthvault.ehv.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        // Ensure all required fields are set
        if (user.getEmailAddress() == null || user.getEmailAddress().trim().isEmpty()) {
            throw new RuntimeException("Email address cannot be empty");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate email uniqueness if changed
        if (!existingUser.getEmailAddress().equals(user.getEmailAddress())) {
            if (userRepository.findByUsername(user.getUsername())
                    .map(u -> !u.getId().equals(user.getId()))
                    .orElse(false)) {
                throw new RuntimeException("Email address is already in use");
            }
        }

        // Update only allowed fields
        existingUser.setName(user.getName());
        existingUser.setEmailAddress(user.getEmailAddress());
        existingUser.setPhoneNumber(user.getPhoneNumber());
        existingUser.setBloodGroup(user.getBloodGroup());

        return userRepository.save(existingUser);
    }
}