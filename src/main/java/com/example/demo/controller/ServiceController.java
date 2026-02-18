package com.example.demo.controller;

import com.example.demo.entity.*;
import com.example.demo.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ServiceController {

    @Autowired private DemandeRepository demandeRepo;
    @Autowired private OffreRepository offreRepo;
    @Autowired private UserRepository userRepo;

    // --- ANCIENNES INTERACTIONS (Maintenues) ---
    @PostMapping("/demande/create")
    public ResponseEntity<?> createDemande(@RequestBody DemandeService d) {
        return ResponseEntity.ok(demandeRepo.save(d));
    }

    // --- FLUX DE L'OFFRE (Nouveau) ---
    @PostMapping("/offre/create")
    public ResponseEntity<?> createOffre(@RequestBody Map<String, Object> req) {
        Offre o = new Offre();
        o.setPrix(Double.valueOf(req.get("prix").toString()));
        o.setTempsArrivee(Integer.valueOf(req.get("temps").toString()));
        o.setServiceType(req.get("serviceType").toString());

        User provider = userRepo.findById(Long.valueOf(req.get("providerId").toString())).get();
        o.setProvider(provider);

        return ResponseEntity.ok(offreRepo.save(o));
    }

    // --- PROFIL DYNAMIQUE (Pour Karim Ahmed -> Réel) ---
    @GetMapping("/auth/me")
    public ResponseEntity<?> getMe(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userRepo.findByUsername(auth.getName()));
    }
}