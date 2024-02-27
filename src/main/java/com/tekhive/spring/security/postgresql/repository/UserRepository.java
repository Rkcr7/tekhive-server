package com.tekhive.spring.security.postgresql.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tekhive.spring.security.postgresql.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u WHERE u.id != :userId AND (LOWER(u.username) LIKE LOWER(concat('%', :query, '%')) OR LOWER(u.fullName) LIKE LOWER(concat('%', :query, '%')))")
    List<User> findUsersByQuery(@Param("query") String query, @Param("userId") Long userId);
    	
    @Query("SELECT u FROM User u WHERE u.id <> :userId AND (LOWER(u.username) LIKE %:query% OR LOWER(u.fullName) LIKE %:query%) AND u.id NOT IN (SELECT f.friend.id FROM Friendship f WHERE f.user.id = :userId AND f.status = 'ACCEPTED')")
    List<User> findUsersByQueryAndNotFriends(@Param("query") String query, @Param("userId") Long userId);
    
//    @Query("SELECT u FROM User u WHERE u.id <> :userId AND (LOWER(u.username) LIKE %:query% OR LOWER(u.fullName) LIKE %:query%) AND u.id NOT IN (SELECT f.friend.id FROM Friendship f WHERE f.user.id = :userId AND f.status = 'ACCEPTED') AND LOWER(u.username) REGEXP :pattern = 1")
//    List<User> findUsersByQueryAndNotFriends(@Param("query") String query, @Param("userId") Long userId, @Param("pattern") String pattern);



    
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}
