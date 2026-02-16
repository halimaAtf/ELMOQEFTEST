package com.example.demo.controller;

import com.example.demo.entity.*;
import com.example.demo.Repository.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "http://localhost:3000")
public class ServiceController {
    private final DemandeRepository demandeRepo;
    private final OffreRepository offreRepo;

    public ServiceController(DemandeRepository d, OffreRepository o) {
        this.demandeRepo = d; this.offreRepo = o;
    }

    @PostMapping("/demande")
    public DemandeService creerDemande(@RequestBody DemandeService d) {
        return demandeRepo.save(d);
    }

    @PostMapping("/offre")
    public Offre faireOffre(@RequestBody Offre o) {
        return offreRepo.save(o);
    }

    @GetMapping("/demandes")
    public List<DemandeService> listerDemandes() {
        return demandeRepo.findAll();
    }
}