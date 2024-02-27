package com.tekhive.spring.security.postgresql.security.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tekhive.spring.security.postgresql.controllers.ResourceNotFoundException;
import com.tekhive.spring.security.postgresql.models.Friendship;
import com.tekhive.spring.security.postgresql.models.FriendshipStatus;
import com.tekhive.spring.security.postgresql.models.User;
import com.tekhive.spring.security.postgresql.repository.FriendshipRepository;
import com.tekhive.spring.security.postgresql.repository.UserRepository;

@Service
public class FriendshipService {
    
    @Autowired
    private FriendshipRepository friendshipRepository;
    
//    public Friendship sendFriendRequest(User user, User friend) {
//        Friendship friendship = friendshipRepository.findByUserAndFriend(user, friend).orElse(null);
//
//        if (friendship == null) {
//            friendship = new Friendship(user, friend, FriendshipStatus.PENDING);
//            friendship.setCreatedAt(LocalDateTime.now());
//            friendshipRepository.save(friendship);
//        } else if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
//            throw new IllegalArgumentException("You have been blocked by this user.");
//        } else if (friendship.getStatus() == FriendshipStatus.PENDING) {
//            throw new IllegalArgumentException("You have already sent a friend request to this user.");
//        } else if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
//            throw new IllegalArgumentException("You are already friends with this user.");
//        }
//
//        return friendship;
//    }

    public Friendship sendFriendRequest(User user, User friend) {
        Friendship friendship = friendshipRepository.findByUserAndFriend(user, friend).orElse(null);
        Friendship reverseFriendship = friendshipRepository.findByUserAndFriend(friend, user).orElse(null);

        if (friendship == null && reverseFriendship == null) {
            // no existing friendship request between users
            Friendship newFriendship = new Friendship(user, friend, FriendshipStatus.PENDING);
            newFriendship.setCreatedAt(LocalDateTime.now());
            friendshipRepository.save(newFriendship);
            return newFriendship;
        } else if (friendship != null && friendship.getStatus() == FriendshipStatus.PENDING) {
            // there is already a pending friendship request from the user to the friend
            throw new IllegalArgumentException("You have already sent a friend request to this user.");
        } else if (reverseFriendship != null && reverseFriendship.getStatus() == FriendshipStatus.PENDING) {
            // there is already a pending friendship request from the friend to the user
            throw new IllegalArgumentException("You have a pending friend request from this user.");
        } else if (friendship != null && friendship.getStatus() != FriendshipStatus.PENDING
                && reverseFriendship != null && reverseFriendship.getStatus() != FriendshipStatus.PENDING) {
            // there is already an accepted or blocked friendship between the users
            throw new IllegalArgumentException("You are already friends with this user or have been blocked by them.");
        } else {
            // there is already a pending friendship request
            throw new IllegalStateException("There is already a pending friendship request.");
        }
    }


    public Friendship acceptFriendRequest(User user, User friend) {
        Friendship friendship = friendshipRepository.findByUserAndFriend(user, friend).orElse(null);
        
        if (friendship == null) {
            throw new IllegalArgumentException("There is no pending friend request from this user.");
        } else if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
            throw new IllegalArgumentException("You have been blocked by this user.");
        } else if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
            throw new IllegalArgumentException("You are already friends with this user.");
        }
        
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
        
        Friendship reverseFriendship = new Friendship(friend, user, FriendshipStatus.ACCEPTED);
        reverseFriendship.setCreatedAt(LocalDateTime.now());
        friendshipRepository.save(reverseFriendship);
        
        return friendship;
    }

    public List<User> getFriends(User user) {
        List<User> friends = new ArrayList<>();
        
        for (Friendship friendship : user.getFriendships()) {
            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                friends.add(friendship.getFriend());
            }
        }
        
        return friends;
    }
    
    @Transactional
    public void removeFriendship(User user, User friend) {
        Friendship friendship = friendshipRepository.findByUserAndFriend(user, friend)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship", "user and friend", user.getUsername() + " and " + friend.getUsername()));

        Friendship reverseFriendship = friendshipRepository.findByUserAndFriend(friend, user)
                .orElse(null);

        friendshipRepository.delete(friendship);

        if (reverseFriendship != null) {
            friendshipRepository.delete(reverseFriendship);
        }
    }
    
    
    public Friendship blockUser(User user, User friend) {
    	Friendship friendship = friendshipRepository.findByUserAndFriend(user, friend).orElse(null);

        
        if (friendship == null) {
            friendship = new Friendship(user, friend, FriendshipStatus.BLOCKED);
            friendship.setCreatedAt(LocalDateTime.now());
            friendshipRepository.save(friendship);
        } else if (friendship.getStatus() == FriendshipStatus.PENDING || friendship.getStatus() == FriendshipStatus.ACCEPTED) {
            friendship.setStatus(FriendshipStatus.BLOCKED);
            friendshipRepository.save(friendship);
        }
        
        Friendship reverseFriendship = friendshipRepository.findByUserAndFriend(friend, user).orElse(null);
        
        if (reverseFriendship != null && reverseFriendship.getStatus() == FriendshipStatus.ACCEPTED) {
            reverseFriendship.setStatus(FriendshipStatus.BLOCKED);
            friendshipRepository.save(reverseFriendship);
        }
        
        return friendship;
    }
    

    
}

