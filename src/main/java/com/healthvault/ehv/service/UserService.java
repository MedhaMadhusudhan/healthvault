package com.healthvault.ehv.service;

import com.healthvault.ehv.model.User;
import com.healthvault.ehv.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

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

        // Validate doctor-specific fields if role is DOCTOR
        if (user.getRole() == User.Role.DOCTOR) {
            if (user.getLicenseNumber() == null || user.getLicenseNumber().trim().isEmpty()) {
                throw new RuntimeException("License number is required for doctors");
            }
            if (user.getSpecialty() == null || user.getSpecialty().trim().isEmpty()) {
                throw new RuntimeException("Specialty is required for doctors");
            }
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findByRole(String role) {
        return userRepository.findByRole(User.Role.valueOf(role));
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
        
        // Update doctor-specific fields if role is DOCTOR
        if (existingUser.getRole() == User.Role.DOCTOR) {
            existingUser.setLicenseNumber(user.getLicenseNumber());
            existingUser.setSpecialty(user.getSpecialty());
        }

        return userRepository.save(existingUser);
    }
    
    public List<User> findAllDoctors() {
        return userRepository.findByRole(User.Role.DOCTOR);
    }
    
    public List<User> findAllPatients() {
        return userRepository.findByRole(User.Role.PATIENT);
    }
}