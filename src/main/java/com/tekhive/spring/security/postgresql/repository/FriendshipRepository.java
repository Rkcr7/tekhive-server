package com.tekhive.spring.security.postgresql.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tekhive.spring.security.postgresql.models.Friendship;
import com.tekhive.spring.security.postgresql.models.FriendshipStatus;
import com.tekhive.spring.security.postgresql.models.User;


@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUserAndStatus(User user, FriendshipStatus status);

    List<Friendship> findByFriendAndStatus(User friend, FriendshipStatus status);

    Optional<Friendship> findByUserAndFriend(User user, User friend);

    List<Friendship> findByUserOrFriendAndStatus(User user, User friend, FriendshipStatus status);

}

