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
    @Autowired private ReviewRepository reviewRepo;

    // ── 1. Client creates request ──
    @PostMapping("/demande/create")
    public ResponseEntity<?> createDemande(@RequestBody DemandeService d, Authentication auth) {
        try {
            User client = userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            d.setClient(client);
            d.setStatus("EN_ATTENTE");
            return ResponseEntity.ok(demandeRepo.save(d));
        } catch (Exception e) {
             e.printStackTrace();
             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    // ── 2. Provider sees available requests (En attente) ──
    @GetMapping("/demande/available")
    public ResponseEntity<?> getAvailableRequests(Authentication auth) {
        User provider = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Provider not found"));
                
        List<DemandeService> enAttente = demandeRepo.findByStatus("EN_ATTENTE");
        
        // Filter by provider's profession if they have one
        if (provider.getProfession() != null && !provider.getProfession().isBlank()) {
           enAttente = enAttente.stream()
               .filter(d -> d.getServiceType() != null && d.getServiceType().equalsIgnoreCase(provider.getProfession()))
               .toList();
        }
        
        return ResponseEntity.ok(enAttente);
    }

    // ── 3. Client sees my requests ──
    @GetMapping("/demande/my")
    public ResponseEntity<?> getMyRequests(Authentication auth) {
        User client = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Client not found"));
        return ResponseEntity.ok(demandeRepo.findByClient_Id(client.getId()));
    }

    // ── 4. Provider creates offer for a request ──
    @PostMapping("/offre/create")
    public ResponseEntity<?> createOffre(@RequestBody Map<String, Object> req, Authentication auth) {
        try {
            Long demandeId = Long.valueOf(req.get("demandeId").toString());
            DemandeService demande = demandeRepo.findById(demandeId)
                    .orElseThrow(() -> new RuntimeException("Demande not found"));

            User provider = userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Provider not found"));

            Offre o = new Offre();
            o.setPrix(Double.valueOf(req.get("prix").toString()));
            o.setTempsArrivee(Integer.valueOf(req.get("tempsArrivee").toString())); 
            o.setDescription((String) req.get("description")); // peut être null
            o.setServiceType(demande.getServiceType()); // ou req.get("serviceType")
            o.setDemande(demande);
            o.setProvider(provider);
            o.setStatus("EN_ATTENTE");

            return ResponseEntity.ok(offreRepo.save(o));
        } catch (Exception e) {
             e.printStackTrace();
             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── 5. Client sees offers for their request ──
    @GetMapping("/demande/{id}/offres")
    public ResponseEntity<List<Offre>> getOffresForDemande(@PathVariable Long id) {
        return ResponseEntity.ok(offreRepo.findByDemande_Id(id));
    }
    
    // ── 6. Client accepts offer ──
    @PostMapping("/offre/{offreId}/accept")
    public ResponseEntity<?> acceptOffre(@PathVariable Long offreId) {
        Offre offre = offreRepo.findById(offreId)
                .orElseThrow(() -> new RuntimeException("Offre not found"));
        DemandeService demande = offre.getDemande();

        // Update Demande
        demande.setStatus("EN_COURS");
        demande.setProvider(offre.getProvider());
        demandeRepo.save(demande);

        // Update Offre
        offre.setStatus("ACCEPTEE");
        offreRepo.save(offre);
        
        // (Optionnel) Rejeter autres offres
        List<Offre> others = offreRepo.findByDemande_Id(demande.getId());
        for (Offre o : others) {
            if (!o.getId().equals(offreId)) {
                o.setStatus("REFUSEE");
                offreRepo.save(o);
            }
        }

        return ResponseEntity.ok(Map.of("message", "Offre acceptée, service en cours"));
    }

    // ── 7. Client leaves review for provider ──
    @PostMapping("/demande/{id}/review")
    public ResponseEntity<?> leaveReview(@PathVariable Long id, @RequestBody Map<String, Object> req, Authentication auth) {
        try {
            DemandeService demande = demandeRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Demande not found"));

            User client = userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Client not found"));

            if (!demande.getClient().getId().equals(client.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
            }

            if (demande.getProvider() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No provider assigned to this demande"));
            }

            // Mark demande as completed if not already
            demande.setStatus("TERMINEE");
            demandeRepo.save(demande);

            Review review = new Review();
            review.setRating(Integer.parseInt(req.get("rating").toString()));
            review.setComment((String) req.get("comment"));
            review.setClient(client);
            review.setProvider(demande.getProvider());
            review.setDemande(demande);

            return ResponseEntity.ok(reviewRepo.save(review));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- PROFIL DYNAMIQUE (Pour Karim Ahmed -> Réel) ---
    @GetMapping("/auth/me")
    public ResponseEntity<?> getMe(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userRepo.findByUsername(auth.getName()));
    }
}