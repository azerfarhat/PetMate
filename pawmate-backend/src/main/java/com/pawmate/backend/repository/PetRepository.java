package com.pawmate.backend.repository;

import com.pawmate.backend.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {
}