package com.petmate.backend.block;

import com.petmate.backend.block.dto.BlockedUserResponse;
import com.petmate.backend.entity.Block;
import com.petmate.backend.entity.User;
import com.petmate.backend.exception.BlockException;
import com.petmate.backend.exception.UserNotFoundException;
import com.petmate.backend.repository.BlockRepository;
import com.petmate.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le blocage est strictement directionnel et personnel : on bloque en son nom,
 * on lève son propre blocage, on ne peut ni se bloquer soi-même ni bloquer un
 * compte inactif. Les deux actions sont idempotentes.
 */
@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    private static final long ME = 1L;
    private static final long OTHER = 2L;

    @Mock
    private BlockRepository blockRepository;
    @Mock
    private UserRepository userRepository;

    private BlockService blockService;

    @BeforeEach
    void setUp() {
        blockService = new BlockService(blockRepository, userRepository);
    }

    @Test
    void block_createsBlockAndReturnsBlockedUser() {
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(user(OTHER)));
        when(blockRepository.findByBlockerIdAndBlockedUserId(ME, OTHER)).thenReturn(Optional.empty());
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BlockedUserResponse response = blockService.block(ME, OTHER);

        assertEquals(OTHER, response.userId());
        assertEquals("Emma", response.firstName());
        verify(blockRepository).save(any(Block.class));
    }

    @Test
    void block_alreadyBlocked_isIdempotentWithoutNewSave() {
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(user(OTHER)));
        when(blockRepository.findByBlockerIdAndBlockedUserId(ME, OTHER))
                .thenReturn(Optional.of(block(ME, OTHER)));

        BlockedUserResponse response = blockService.block(ME, OTHER);

        assertEquals(OTHER, response.userId());
        verify(blockRepository, never()).save(any(Block.class));
    }

    @Test
    void block_self_throwsBlockException() {
        assertThrows(BlockException.class, () -> blockService.block(ME, ME));
    }

    @Test
    void block_unknownUser_throwsNotFound() {
        when(userRepository.findById(OTHER)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> blockService.block(ME, OTHER));
    }

    @Test
    void block_inactiveUser_throwsNotFound() {
        User inactive = user(OTHER);
        inactive.setActive(false);
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(inactive));

        assertThrows(UserNotFoundException.class, () -> blockService.block(ME, OTHER));
    }

    @Test
    void unblock_removesDirectedBlockOnly() {
        blockService.unblock(ME, OTHER);

        verify(blockRepository).deleteByBlockerIdAndBlockedUserId(ME, OTHER);
    }

    @Test
    void unblock_whenNothingToRemove_isIdempotent() {
        blockService.unblock(ME, OTHER);

        verify(blockRepository).deleteByBlockerIdAndBlockedUserId(ME, OTHER);
    }

    @Test
    void blockedUsers_returnsBlockedUsersNewestFirst() {
        when(blockRepository.findBlockedUsers(ME)).thenReturn(List.of(block(ME, 5L), block(ME, 6L)));

        List<BlockedUserResponse> response = blockService.blockedUsers(ME);

        assertEquals(2, response.size());
        assertEquals(5L, response.get(0).userId());
        assertEquals("Khalid", response.get(1).firstName());
    }

    private Block block(Long blockerId, Long blockedId) {
        return Block.builder()
                .id(blockedId)
                .blocker(User.builder().id(blockerId).build())
                .blockedUser(user(blockedId))
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .firstName(id == OTHER ? "Emma" : "Khalid")
                .lastName("Test")
                .active(true)
                .build();
    }
}