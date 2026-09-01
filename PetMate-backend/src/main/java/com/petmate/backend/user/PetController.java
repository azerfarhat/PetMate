package com.petmate.backend.user;

import com.petmate.backend.security.AuthUserPrincipal;
import com.petmate.backend.user.dto.PetRequest;
import com.petmate.backend.user.dto.PetResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST des Pet de l'utilisateur connecté (protégée par JWT). L'identifiant
 * de l'utilisateur est pris du principal authentifié, jamais de l'URL.
 *
 * Même payload {@link PetRequest} que l'inscription : le wizard (espèce, race,
 * âge, énergie) est identique à la création d'un compte et à l'ajout d'un Pet.
 */
@RestController
@RequestMapping("/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    /**
     * Tous les Pet actifs de l'utilisateur connecté.
     */
    @GetMapping
    public ResponseEntity<List<PetResponse>> listMine(Authentication authentication) {
        return ResponseEntity.ok(petService.listOwnedPets(userId(authentication)));
    }

    /**
     * Toutes les informations d'un Pet possédé par l'utilisateur connecté.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PetResponse> getPet(Authentication authentication,
                                              @PathVariable("id") Long id) {
        return ResponseEntity.ok(petService.getOwnedPet(userId(authentication), id));
    }

    /**
     * Ajout d'un nouveau Pet (wizard) : actif immédiatement pour le swipe.
     */
    @PostMapping
    public ResponseEntity<PetResponse> createPet(Authentication authentication,
                                                 @Valid @RequestBody PetRequest request) {
        PetResponse created = petService.createOwnedPet(userId(authentication), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Modification d'un Pet (wizard) : remplacement complet du payload
     * {@link PetRequest} (champs + photos). PUT est utilisé car la ressource
     * entière est remplacée, contrairement à une mise à jour partielle PATCH.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PetResponse> updatePet(Authentication authentication,
                                                 @PathVariable("id") Long id,
                                                 @Valid @RequestBody PetRequest request) {
        return ResponseEntity.ok(petService.updateOwnedPet(userId(authentication), id, request));
    }

    /**
     * Suppression en douceur du Pet : il disparaît du feed et de la liste, mais
     * l'historique des matchs n'est pas détruit. Refusée s'il s'agit du dernier
     * Pet actif.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(Authentication authentication,
                                          @PathVariable("id") Long id) {
        petService.deleteOwnedPet(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private long userId(Authentication authentication) {
        return ((AuthUserPrincipal) authentication.getPrincipal()).getUserId();
    }
}