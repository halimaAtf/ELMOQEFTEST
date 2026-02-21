package com.example.demo.Repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    List<User> findByRoleAndStatus(String role, String status);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    long countByRoleAndStatus(String role, String status);
}