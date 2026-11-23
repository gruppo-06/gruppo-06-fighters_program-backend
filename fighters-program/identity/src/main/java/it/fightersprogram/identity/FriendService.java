package it.fightersprogram.identity;

import java.util.Comparator;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.common.error.BadRequestException;
import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.identity.domain.Friendship;
import it.fightersprogram.identity.domain.Friendship.Status;
import it.fightersprogram.identity.domain.FriendshipRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserDirectory userDirectory;

    public List<FriendView> friends(long userId) {
        return friendshipRepository.findByStatusInvolving(userId, Status.ACCEPTED).stream()
                .map(friendship -> new FriendView(nicknameOfOther(friendship, userId), friendship.getAcceptedAt()))
                .sorted(Comparator.comparing(FriendView::nickname))
                .toList();
    }

    public FriendRequests requests(long userId) {
        List<FriendRequestView> incoming = friendshipRepository
                .findByAddresseeIdAndStatusOrderByCreatedAtDesc(userId, Status.PENDING).stream()
                .map(friendship -> requestView(friendship, userId))
                .toList();
        List<FriendRequestView> outgoing = friendshipRepository
                .findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, Status.PENDING).stream()
                .map(friendship -> requestView(friendship, userId))
                .toList();
        return new FriendRequests(incoming, outgoing);
    }

    @Transactional
    public FriendRequestView request(long requesterId, String nickname) {
        long addresseeId = userDirectory.findIdByNickname(nickname)
                .orElseThrow(() -> new NotFoundException("User not found: " + nickname));
        if (requesterId == addresseeId) {
            throw new BadRequestException("You cannot send a friend request to yourself");
        }
        friendshipRepository.findBetween(requesterId, addresseeId).ifPresent(existing -> {
            if (existing.getStatus() == Status.ACCEPTED) {
                throw new ConflictException("You are already friends with " + nickname);
            }
            throw new ConflictException("A friend request with " + nickname + " is already pending");
        });
        Friendship friendship;
        try {
            friendship = friendshipRepository.saveAndFlush(Friendship.request(requesterId, addresseeId));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("A friend request with " + nickname + " is already pending");
        }
        return requestView(friendship, requesterId);
    }

    @Transactional
    public FriendView accept(long userId, long requestId) {
        Friendship friendship = friendshipRepository.findByIdAndAddresseeId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Friend request not found: " + requestId));
        friendship.accept(userId);
        return new FriendView(nicknameOfOther(friendship, userId), friendship.getAcceptedAt());
    }

    @Transactional
    public void decline(long userId, long requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .filter(candidate -> candidate.involves(userId) && candidate.getStatus() == Status.PENDING)
                .orElseThrow(() -> new NotFoundException("Friend request not found: " + requestId));
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void remove(long userId, String nickname) {
        long otherId = userDirectory.findIdByNickname(nickname)
                .orElseThrow(() -> new NotFoundException("User not found: " + nickname));
        Friendship friendship = friendshipRepository.findBetween(userId, otherId)
                .filter(candidate -> candidate.getStatus() == Status.ACCEPTED)
                .orElseThrow(() -> new NotFoundException("You are not friends with " + nickname));
        friendshipRepository.delete(friendship);
    }

    private FriendRequestView requestView(Friendship friendship, long userId) {
        return new FriendRequestView(friendship.getId(), nicknameOfOther(friendship, userId),
                friendship.getCreatedAt());
    }

    private String nicknameOfOther(Friendship friendship, long userId) {
        return userDirectory.nicknameOf(friendship.otherUserOf(userId));
    }
}
