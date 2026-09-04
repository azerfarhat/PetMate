package com.petmate.backend.block;

import com.petmate.backend.block.dto.BlockedUserResponse;
import com.petmate.backend.entity.Block;
import com.petmate.backend.entity.User;
import com.petmate.backend.exception.BlockException;
import com.petmate.backend.exception.UserNotFoundException;
import com.petmate.backend.repository.BlockRepository;
import com.petmate.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Blocage entre utilisateurs. Le blocage est directionnel : chaque utilisateur
 * lève son propre blocage. Une fois posé, il est pris en compte par le feed
 * ({@code findCandidates}) et la messagerie ({@code MessageService}).
 *
 * <p>Règles : on ne peut pas se bloquer soi-même, la cible doit exister et être
 * un compte actif. Bloqueur et débocage sont idempotents (aucune erreur sur un
 * double appel).</p>
 */
@Service
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    public BlockService(BlockRepository blockRepository, UserRepository userRepository) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
    }

    /**
     * Bloque {@code blockedUserId}. Idempotent : un blocage déjà posé dans le
     * même sens est conservé et renvoyé tel quel, aucun doublon n'est créé.
     */
    @Transactional
    public BlockedUserResponse block(Long blockerId, Long blockedUserId) {
        if (blockerId.equals(blockedUserId)) {
            throw new BlockException("Impossible de se bloquer soi-même");
        }

        User blocked = userRepository.findById(blockedUserId)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        Block block = blockRepository.findByBlockerIdAndBlockedUserId(blockerId, blockedUserId)
                .orElseGet(() -> blockRepository.save(Block.builder()
                        .blocker(User.builder().id(blockerId).build())
                        .blockedUser(blocked)
                        .build()));

        return toResponse(block);
    }

    /**
     * Lève le blocage posé par {@code blockerId} vers {@code blockedUserId}.
     * Idempotent : débocaquer quelqu'un qu'on ne bloque pas n'est pas une erreur.
     */
    @Transactional
    public void unblock(Long blockerId, Long blockedUserId) {
        blockRepository.deleteByBlockerIdAndBlockedUserId(blockerId, blockedUserId);
    }

    /**
     * Liste des utilisateurs bloqués par {@code blockerId}, plus récents d'abord.
     */
    @Transactional(readOnly = true)
    public List<BlockedUserResponse> blockedUsers(Long blockerId) {
        return blockRepository.findBlockedUsers(blockerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private BlockedUserResponse toResponse(Block block) {
        User blocked = block.getBlockedUser();
        return new BlockedUserResponse(
                blocked.getId(),
                blocked.getFirstName(),
                blocked.getLastName(),
                blocked.getProfilePicture(),
                block.getCreatedAt());
    }
}