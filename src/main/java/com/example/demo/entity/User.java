package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;

    // getters & setters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() {return Email;}
    public void setEmail(String Email){this.Email = Email;}
    public String getPhone() {return phone;}
    public void setPhone(String phone){this.phone = phone;}
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isValidated() { return isValidated; }
    public void setValidated(boolean validated) { isValidated = validated; }

}
