package com.petmate.backend.repository;

import com.petmate.backend.entity.PetPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetPhotoRepository extends JpaRepository<PetPhoto, Long> {
}