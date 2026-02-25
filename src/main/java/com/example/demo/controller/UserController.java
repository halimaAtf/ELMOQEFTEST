package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5174", allowCredentials = "true")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserProfile(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            Optional<User> optionalUser = userRepository.findById(id);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            User user = optionalUser.get();

            if (updates.containsKey("username")) {
                user.setUsername((String) updates.get("username"));
            }
            if (updates.containsKey("email")) {
                user.setEmail((String) updates.get("email"));
            }
            if (updates.containsKey("phone")) {
                user.setPhone((String) updates.get("phone"));
            }
            if (updates.containsKey("profession")) {
                user.setProfession((String) updates.get("profession"));
            }
            if (updates.containsKey("profilePicture")) {
                user.setProfilePicture((String) updates.get("profilePicture"));
            }
            if (updates.containsKey("latitude")) {
                Object lat = updates.get("latitude");
                if (lat instanceof Number) {
                    user.setLatitude(((Number) lat).doubleValue());
                } else if (lat instanceof String) {
                    user.setLatitude(Double.valueOf((String) lat));
                }
            }
            if (updates.containsKey("longitude")) {
                Object lng = updates.get("longitude");
                if (lng instanceof Number) {
                    user.setLongitude(((Number) lng).doubleValue());
                } else if (lng instanceof String) {
                    user.setLongitude(Double.valueOf((String) lng));
                }
            }

            userRepository.save(user);

            Map<String, Object> response = new java.util.HashMap<>(Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "phone", user.getPhone() != null ? user.getPhone() : "",
                    "role", user.getRole(),
                    "status", user.getStatus(),
                    "profession", user.getProfession() != null ? user.getProfession() : ""));

            if (user.getProfilePicture() != null) {
                response.put("profilePicture", user.getProfilePicture());
            }
            if (user.getLatitude() != null) {
                response.put("latitude", user.getLatitude());
            }
            if (user.getLongitude() != null) {
                response.put("longitude", user.getLongitude());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Erreur lors de la mise à jour"));
        }
    }
}
