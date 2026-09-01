package com.petmate.backend.match.dto;

import com.petmate.backend.enums.MatchStatus;
import com.petmate.backend.enums.PetType;

import java.time.LocalDateTime;

/**
 * Un match avec les Pet des deux owners et la conversation automatiquement
 * ouverte. Symétrique : le client identifie quelle Pet est la sienne.
 */
public record MatchResponse(
        Long matchId,
        MatchStatus status,
        LocalDateTime matchedAt,
        Long conversationId,
        PetSummary pet1,
        PetSummary pet2) {

    public record PetSummary(Long petId, String petName, String primaryPhotoUrl,
                             PetType type, Integer age, String breed) {
    }
}