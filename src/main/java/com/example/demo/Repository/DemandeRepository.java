package com.example.demo.Repository;

import com.example.demo.entity.DemandeService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeRepository extends JpaRepository<DemandeService, Long> {
    long countByStatus(String status);


    List<DemandeService> findByClient_Id(Long clientId);
    List<DemandeService> findByProvider_Id(Long providerId);
    List<DemandeService> findByStatus(String status);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(o.prix) FROM DemandeService d JOIN d.offres o WHERE d.status IN ('TERMINE', 'TERMINEE') AND o.status = 'ACCEPTED'")
    Double sumRevenueFromCompletedJobs();

    @org.springframework.data.jpa.repository.Query("SELECT FUNCTION('MONTHNAME', d.createdAt) as month, SUM(o.prix) as revenue, COUNT(d.id) as jobs FROM DemandeService d JOIN d.offres o WHERE d.status IN ('TERMINE', 'TERMINEE') AND o.status = 'ACCEPTED' AND FUNCTION('YEAR', d.createdAt) = FUNCTION('YEAR', CURRENT_DATE) GROUP BY FUNCTION('MONTHNAME', d.createdAt)")
    List<Object[]> getMonthlyStats();
}