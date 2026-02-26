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

    @Autowired
    private DemandeRepository demandeRepo;
    @Autowired
    private OffreRepository offreRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private ReviewRepository reviewRepo;
    @Autowired
    private SupportTicketRepository supportRepo;
    @Autowired
    private NotificationRepository notificationRepo;

    // Client creates request
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
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Provider sees available requests (En attente) ──
    @GetMapping("/demande/available")
    public ResponseEntity<?> getAvailableRequests(Authentication auth) {
        User provider = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        List<DemandeService> enAttente = demandeRepo.findByStatus("EN_ATTENTE");

        // 1. Filter by provider's profession (category)
        if (provider.getProfession() != null && !provider.getProfession().isBlank()) {
            enAttente = enAttente.stream()
                    .filter(d -> d.getServiceType() != null
                            && d.getServiceType().equalsIgnoreCase(provider.getProfession()))
                    .toList();
        }

        // 2. Hide requests where the provider already has an offer waiting for response
        enAttente = enAttente.stream().filter(d -> {
            if (d.getOffres() == null)
                return true;
            for (Offre o : d.getOffres()) {
                if (o.getProvider() != null && o.getProvider().getId().equals(provider.getId())) {
                    if ("EN_ATTENTE".equalsIgnoreCase(o.getStatus()) || "ACTIVE".equalsIgnoreCase(o.getStatus())) {
                        return false; // Offer already sent
                    }
                }
            }
            return true;
        }).toList();

        // 3. Filter by distance (50km radius) if provider and request have location
        if (provider.getLatitude() != null && provider.getLongitude() != null) {
            enAttente = enAttente.stream().filter(d -> {
                if (d.getLatitude() != null && d.getLongitude() != null) {
                    double distance = calculateDistance(
                            provider.getLatitude(), provider.getLongitude(),
                            d.getLatitude(), d.getLongitude());
                    d.setDistance(distance); // Set transient field for frontend
                    return distance <= 50.0;
                }
                return false;
            }).toList();
        } else {
            // If provider has no location set, do not show requests to avoid showing
            // requests from other cities
            enAttente = new ArrayList<>();
        }

        return ResponseEntity.ok(enAttente);
    }

    // Client sees my requests ──
    @GetMapping("/demande/my")
    public ResponseEntity<?> getMyRequests(Authentication auth) {
        User client = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Client not found"));
        return ResponseEntity.ok(demandeRepo.findByClient_Id(client.getId()));
    }

    // Provider sees their assigned requests
    @GetMapping("/demande/provider/my")
    public ResponseEntity<?> getProviderRequests(Authentication auth) {
        User provider = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        return ResponseEntity.ok(demandeRepo.findByProvider_Id(provider.getId()));
    }

    // Provider creates offer for a request
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

            Offre saved = offreRepo.save(o);

            Notification notif = new Notification();
            notif.setUser(demande.getClient());
            notif.setMessage("Nouvelle offre de " + provider.getUsername() + " pour votre demande de "
                    + demande.getServiceType());
            notificationRepo.save(notif);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Client sees offers for their request ──
    @GetMapping("/demande/{id}/offres")
    public ResponseEntity<List<Offre>> getOffresForDemande(@PathVariable Long id) {
        DemandeService demande = demandeRepo.findById(id).orElse(null);
        List<Offre> activeOffers = offreRepo.findByDemande_Id(id).stream()
                .filter(o -> !"REFUSEE".equalsIgnoreCase(o.getStatus()))
                .peek(o -> {
                    if (demande != null && demande.getLatitude() != null && demande.getLongitude() != null &&
                            o.getProvider().getLatitude() != null && o.getProvider().getLongitude() != null) {
                        double distance = calculateDistance(
                                demande.getLatitude(), demande.getLongitude(),
                                o.getProvider().getLatitude(), o.getProvider().getLongitude());
                        o.setDistance(distance);
                    }

                    List<Review> reviews = reviewRepo.findByProviderId(o.getProvider().getId());
                    double avgRating = 5.0;
                    if (reviews != null && !reviews.isEmpty()) {
                        avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(5.0);
                    }
                    o.setProviderRating(Math.round(avgRating * 10.0) / 10.0);

                    List<DemandeService> providerJobs = demandeRepo.findByProvider_Id(o.getProvider().getId());
                    int jobsDone = 0;
                    if (providerJobs != null) {
                        for (DemandeService ds : providerJobs) {
                            if ("TERMINEE".equalsIgnoreCase(ds.getStatus())
                                    || "TERMINE".equalsIgnoreCase(ds.getStatus())) {
                                jobsDone++;
                            }
                        }
                    }
                    o.setProviderJobsDone(jobsDone);
                })
                .toList();
        return ResponseEntity.ok(activeOffers);
    }

    // accepts offer
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

        // Rejeter autres offres
        List<Offre> others = offreRepo.findByDemande_Id(demande.getId());
        for (Offre o : others) {
            if (!o.getId().equals(offreId)) {
                o.setStatus("REFUSEE");
                offreRepo.save(o);

                Notification refNotif = new Notification();
                refNotif.setUser(o.getProvider());
                refNotif.setMessage("Votre offre pour " + demande.getServiceType() + " a été refusée.");
                notificationRepo.save(refNotif);
            }
        }

        Notification accNotif = new Notification();
        accNotif.setUser(offre.getProvider());
        accNotif.setMessage("Félicitations! Votre offre pour " + demande.getServiceType() + " a été acceptée.");
        notificationRepo.save(accNotif);

        return ResponseEntity.ok(Map.of("message", "Offre acceptée, service en cours"));
    }

    @PostMapping("/offre/{offreId}/reject")
    public ResponseEntity<?> rejectOffre(@PathVariable Long offreId) {
        Offre offre = offreRepo.findById(offreId)
                .orElseThrow(() -> new RuntimeException("Offre not found"));
        DemandeService demande = offre.getDemande();

        offre.setStatus("REFUSEE");
        offreRepo.save(offre);

        Notification notif = new Notification();
        notif.setUser(offre.getProvider());
        notif.setMessage("Votre offre pour " + demande.getServiceType() + " a été refusée par le client.");
        notificationRepo.save(notif);

        return ResponseEntity.ok(Map.of("message", "Offre refusée"));
    }

    // Client leaves review for provider
    @PostMapping("/demande/{id}/review")
    public ResponseEntity<?> leaveReview(@PathVariable Long id, @RequestBody Map<String, Object> req,
            Authentication auth) {
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

            List<Review> existingReviews = reviewRepo.findByDemandeId(id);
            Review review;
            if (!existingReviews.isEmpty()) {
                review = existingReviews.get(0);
            } else {
                review = new Review();
                review.setClient(client);
                review.setProvider(demande.getProvider());
                review.setDemande(demande);
            }

            Object ratingObj = req.get("rating");
            if (ratingObj != null) {
                review.setRating(ratingObj instanceof Number ? ((Number) ratingObj).intValue()
                        : Integer.parseInt(ratingObj.toString()));
            }
            review.setComment((String) req.get("comment"));

            return ResponseEntity.ok(reviewRepo.save(review));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/demande/{id}/cancel")
    public ResponseEntity<?> cancelDemande(@PathVariable Long id, Authentication auth) {
        try {
            User user = userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            DemandeService demande = demandeRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Demande not found"));

            // Allow client or assigned provider to cancel
            boolean isClient = demande.getClient().getId().equals(user.getId());
            boolean isProvider = demande.getProvider() != null && demande.getProvider().getId().equals(user.getId());
            if (!isClient && !isProvider) {
                return ResponseEntity.status(403).body(Map.of("error", "Unauthorized to cancel this request"));
            }

            demande.setStatus("ANNULEE");
            demandeRepo.save(demande);

            // Cancel all offers related to this demand
            List<Offre> offers = offreRepo.findByDemande_Id(demande.getId());
            if (offers != null) {
                for (Offre o : offers) {
                    if (!"REFUSEE".equalsIgnoreCase(o.getStatus())) {
                        o.setStatus("ANNULEE");
                        offreRepo.save(o);
                    }
                }
            }

            // Notify the other party
            User otherParty = isClient ? demande.getProvider() : demande.getClient();
            if (otherParty != null) {
                Notification notif = new Notification();
                notif.setUser(otherParty);
                notif.setMessage("La demande pour " + demande.getServiceType() + " a été annulée par "
                        + user.getUsername() + ".");
                notificationRepo.save(notif);
            }

            return ResponseEntity.ok(Map.of("success", true, "status", "ANNULEE"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/demande/{id}/complete")
    public ResponseEntity<?> completeDemande(@PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> req, Authentication auth) {
        try {
            User provider = userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Provider not found"));
            DemandeService demande = demandeRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Demande not found"));

            if (demande.getProvider() == null || !demande.getProvider().getId().equals(provider.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Unauthorized to complete this request"));
            }

            demande.setStatus("TERMINEE");
            demandeRepo.save(demande);

            if (req != null && req.containsKey("rating")) {
                List<Review> existingReviews = reviewRepo.findByDemandeId(id);
                Review review;
                if (!existingReviews.isEmpty()) {
                    review = existingReviews.get(0);
                } else {
                    review = new Review();
                    review.setClient(demande.getClient());
                    review.setProvider(provider);
                    review.setDemande(demande);
                }

                Object ratingObj = req.get("rating");
                if (ratingObj != null) {
                    review.setClientRating(ratingObj instanceof Number ? ((Number) ratingObj).intValue()
                            : Integer.parseInt(ratingObj.toString()));
                }
                review.setClientComment((String) req.get("comment"));
                reviewRepo.save(review);
            }

            // Notify client
            if (demande.getClient() != null) {
                Notification notif = new Notification();
                notif.setUser(demande.getClient());
                notif.setMessage("Le prestataire " + provider.getUsername() + " a terminé votre demande de "
                        + demande.getServiceType() + ". Veuillez laisser un avis.");
                notificationRepo.save(notif);
            }

            return ResponseEntity.ok(Map.of("message", "Demande marked as completed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> getMe(Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userRepo.findByUsername(auth.getName()));
    }

    @PostMapping("/support")
    public ResponseEntity<?> createSupportTicket(@RequestBody Map<String, String> req, Authentication auth) {
        try {
            User user = userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            SupportTicket ticket = new SupportTicket();
            ticket.setUser(user);
            ticket.setMessage(req.get("message"));
            return ResponseEntity.ok(supportRepo.save(ticket));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/provider/stats")
    public ResponseEntity<?> getProviderStats(Authentication auth) {
        try {
            User provider = userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Provider not found"));

            List<DemandeService> myDemandes = demandeRepo.findByProvider_Id(provider.getId());
            int jobsDone = 0;
            double earnings = 0.0;

            for (DemandeService ds : myDemandes) {
                if ("TERMINEE".equalsIgnoreCase(ds.getStatus()) || "TERMINE".equalsIgnoreCase(ds.getStatus())) {
                    jobsDone++;

                    if (ds.getOffres() != null) {
                        for (Offre o : ds.getOffres()) {
                            if ("ACCEPTEE".equalsIgnoreCase(o.getStatus())
                                    || "ACCEPTED".equalsIgnoreCase(o.getStatus())) {
                                earnings += o.getPrix();
                            }
                        }
                    }
                }
            }

            List<Review> reviews = reviewRepo.findByProviderId(provider.getId());
            double averageRating = 5.0; // Default to 5.0 if no reviews
            if (reviews != null && !reviews.isEmpty()) {
                averageRating = reviews.stream().mapToInt(Review::getRating).average().orElse(5.0);
            }

            averageRating = Math.round(averageRating * 10.0) / 10.0; // Round to 1 decimal
            int reviewCount = (reviews != null) ? reviews.size() : 0;

            return ResponseEntity.ok(Map.of(
                    "jobsDone", jobsDone,
                    "earnings", earnings,
                    "rating", averageRating,
                    "reviewCount", reviewCount));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/client/stats")
    public ResponseEntity<?> getClientStats(Authentication auth) {
        try {
            User client = userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Client not found"));

            List<DemandeService> myDemandes = demandeRepo.findByClient_Id(client.getId());
            int requestsMade = myDemandes.size();
            double totalSpent = 0.0;

            for (DemandeService ds : myDemandes) {
                if ("TERMINEE".equalsIgnoreCase(ds.getStatus()) || "TERMINE".equalsIgnoreCase(ds.getStatus())) {
                    if (ds.getOffres() != null) {
                        for (Offre o : ds.getOffres()) {
                            if ("ACCEPTEE".equalsIgnoreCase(o.getStatus())
                                    || "ACCEPTED".equalsIgnoreCase(o.getStatus())) {
                                totalSpent += o.getPrix();
                            }
                        }
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                    "requestsMade", requestsMade,
                    "totalSpent", totalSpent,
                    "rating", 5.0,
                    "reviewCount", 0));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/provider/{username}/profile")
    public ResponseEntity<?> getProviderProfile(@PathVariable String username) {
        try {
            User provider = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Provider not found"));

            List<DemandeService> myDemandes = demandeRepo.findByProvider_Id(provider.getId());
            int jobsDone = 0;
            double earnings = 0.0;
            for (DemandeService ds : myDemandes) {
                if ("TERMINEE".equalsIgnoreCase(ds.getStatus()) || "TERMINE".equalsIgnoreCase(ds.getStatus())) {
                    jobsDone++;
                    if (ds.getOffres() != null) {
                        for (Offre o : ds.getOffres()) {
                            if ("ACCEPTEE".equalsIgnoreCase(o.getStatus())
                                    || "ACCEPTED".equalsIgnoreCase(o.getStatus())) {
                                earnings += o.getPrix();
                            }
                        }
                    }
                }
            }

            List<Review> reviews = reviewRepo.findByProviderId(provider.getId());
            double averageRating = 5.0;
            int reviewCount = 0;
            List<Map<String, Object>> reviewDTOs = new ArrayList<>();
            if (reviews != null && !reviews.isEmpty()) {
                averageRating = reviews.stream().mapToInt(Review::getRating).average().orElse(5.0);
                averageRating = Math.round(averageRating * 10.0) / 10.0;
                reviewCount = reviews.size();
                for (Review r : reviews) {
                    reviewDTOs.add(Map.of(
                            "id", r.getId(),
                            "rating", r.getRating(),
                            "comment", r.getComment() != null ? r.getComment() : "",
                            "createdAt", r.getCreatedAt().toString()));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "id", provider.getId(),
                    "username", provider.getUsername(),
                    "profession", provider.getProfession() != null ? provider.getProfession() : provider.getRole(),
                    "phone", provider.getPhone() != null ? provider.getPhone() : "",
                    "profilePicture", provider.getProfilePicture() != null ? provider.getProfilePicture() : "",
                    "jobsDone", jobsDone,
                    "earnings", earnings,
                    "rating", averageRating,
                    "reviewCount", reviewCount,
                    "reviews", reviewDTOs));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}