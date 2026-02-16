package com.example.demo.Repository;
import com.example.demo.entity.Offre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OffreRepository extends JpaRepository<Offre, Long> {
    List<Offre> findByDemandeId(Long demandeId);

}