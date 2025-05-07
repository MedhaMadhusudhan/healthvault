package com.healthvault.ehv.repository;

import com.healthvault.ehv.model.User;
import com.healthvault.ehv.model.User.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    List<User> findByRole(Role role);
}