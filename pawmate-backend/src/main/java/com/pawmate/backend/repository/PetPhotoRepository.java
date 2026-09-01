package com.pawmate.backend.repository;

import com.pawmate.backend.entity.PetPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetPhotoRepository extends JpaRepository<PetPhoto, Long> {
}