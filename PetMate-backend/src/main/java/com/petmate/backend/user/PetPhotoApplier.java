package com.petmate.backend.user;

import com.petmate.backend.entity.Pet;
import com.petmate.backend.entity.PetPhoto;
import com.petmate.backend.exception.PetNotFoundException;
import com.petmate.backend.repository.PetPhotoRepository;
import com.petmate.backend.user.dto.PhotoUpdateRequest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application des photos d'un Pet selon une sémantique de remplacement total :
 * la liste fournie remplace les photos existantes (création des nouvelles,
 * mise à jour des existantes, suppression en masse de celles absentes).
 *
 * Garantit l'invariant "une seule photo principale" : si plusieurs
 * {@code primaryPhoto} sont déclarées, la première (par {@code displayOrder}
 * puis ordre de la requête) l'emporte ; si aucune n'est déclarée, la première
 * photo devient principale.
 *
 * La collection de {@code photos} du {@link Pet} est maintenue cohérente
 * (ajouts et retraits), ce qui évite à l'appelant un rechargement dans le
 * chemin nominal.
 */
@Component
public final class PetPhotoApplier {

    private final PetPhotoRepository petPhotoRepository;

    public PetPhotoApplier(PetPhotoRepository petPhotoRepository) {
        this.petPhotoRepository = petPhotoRepository;
    }

    /**
     * Remplace la liste de photos d'un {@code pet} par {@code photoRequests}.
     */
    public void replaceAll(Pet pet, List<PhotoUpdateRequest> photoRequests) {
        List<PhotoUpdateRequest> requests = photoRequests == null ? List.of() : photoRequests;

        Map<Long, PetPhoto> existingById = pet.getPhotos().stream()
                .collect(Collectors.toMap(PetPhoto::getId, Function.identity()));

        Set<Long> keptPhotoIds = requests.stream()
                .map(PhotoUpdateRequest::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long keptId : keptPhotoIds) {
            if (!existingById.containsKey(keptId)) {
                throw new PetNotFoundException("Photo introuvable");
            }
        }

        List<PhotoUpdateRequest> ordered = requests.stream()
                .sorted(Comparator.comparing(PhotoUpdateRequest::displayOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
        PhotoUpdateRequest defaultPrimary = ordered.isEmpty() ? null
                : ordered.stream().filter(PhotoUpdateRequest::primaryPhoto).findFirst()
                        .orElse(ordered.get(0));

        for (PhotoUpdateRequest request : requests) {
            boolean primary = request == defaultPrimary;
            if (request.id() == null) {
                PetPhoto created = buildPhoto(pet, request, primary);
                pet.getPhotos().add(created);
            } else {
                PetPhoto photo = existingById.get(request.id());
                photo.setUrl(request.url().trim());
                photo.setPrimaryPhoto(primary);
                photo.setDisplayOrder(request.displayOrder());
            }
        }

        List<Long> photoIdsToDelete = existingById.keySet().stream()
                .filter(id -> !keptPhotoIds.contains(id))
                .toList();

        if (!photoIdsToDelete.isEmpty()) {
            petPhotoRepository.deleteByPetIdAndPhotoIds(pet.getId(), photoIdsToDelete);
            pet.getPhotos().removeIf(photo -> photoIdsToDelete.contains(photo.getId()));
        }
    }

    /**
     * Supprime en masse les photos des Pet supprimés, en une requête SQL.
     */
    public void deleteAllByPetIds(Collection<Long> petIds) {
        if (petIds == null || petIds.isEmpty()) {
            return;
        }
        petPhotoRepository.deleteByPetIds(petIds);
    }

    private PetPhoto buildPhoto(Pet pet, PhotoUpdateRequest request, boolean primary) {
        return petPhotoRepository.save(PetPhoto.builder()
                .url(request.url().trim())
                .primaryPhoto(primary)
                .displayOrder(request.displayOrder())
                .pet(pet)
                .build());
    }
}