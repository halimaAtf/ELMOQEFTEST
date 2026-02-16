package com.example.demo.Repository;
import com.example.demo.entity.DemandeService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandeRepository extends JpaRepository<DemandeService, Long> {}