package com.example.demo.Repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Trouver un utilisateur par son nom d'utilisateur
    Optional<User> findByUsername(String username);

    // Trouver les utilisateurs par rôle et statut de validation
    List<User> findByRoleAndIsValidated(String role, boolean isValidated);
}