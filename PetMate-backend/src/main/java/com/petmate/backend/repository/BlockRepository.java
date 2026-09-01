package com.petmate.backend.repository;

import com.petmate.backend.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockRepository extends JpaRepository<Block, Long> {

    /**
     * Vrai si un blocage existe entre deux owners, dans un sens ou l'autre.
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Block b
            WHERE (b.blocker.id = :firstId AND b.blockedUser.id = :secondId)
               OR (b.blocker.id = :secondId AND b.blockedUser.id = :firstId)
            """)
    boolean existBetweenOwners(@Param("firstId") Long firstId, @Param("secondId") Long secondId);
}