package com.tekhive.spring.security.postgresql.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import javax.management.AttributeNotFoundException;
import javax.validation.Valid;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tekhive.spring.security.postgresql.models.Friendship;
import com.tekhive.spring.security.postgresql.models.FriendshipStatus;
import com.tekhive.spring.security.postgresql.models.User;
import com.tekhive.spring.security.postgresql.payload.request.LoginRequest;
import com.tekhive.spring.security.postgresql.payload.request.SignupRequest;
import com.tekhive.spring.security.postgresql.payload.request.UserSummary;
import com.tekhive.spring.security.postgresql.payload.request.UserUpdateRequest;
import com.tekhive.spring.security.postgresql.payload.response.JwtResponse;
import com.tekhive.spring.security.postgresql.payload.response.MessageResponse;
import com.tekhive.spring.security.postgresql.repository.FriendshipRepository;
import com.tekhive.spring.security.postgresql.repository.UserRepository;
import com.tekhive.spring.security.postgresql.security.jwt.JwtUtils;
import com.tekhive.spring.security.postgresql.security.services.FriendshipService;
import com.tekhive.spring.security.postgresql.security.services.UserDetailsImpl;

//@CrossOrigin(origins = "*", maxAge = 3600)
//@CrossOrigin(origins = "http://localhost:3000")
//@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600)
//@CrossOrigin(origins = "http://127.0.0.1:5173", maxAge = 3600)

//@CrossOrigin(origins = "http://127.0.0.1:5173")
@CrossOrigin(origins = "*")

@RestController

@RequestMapping("/api/auth")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;
	
	@Autowired
	FriendshipService friendshipService;  // <-- Add this

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;



	private static final String PROFILE_IMAGES_DIR = "profile-images/";

	@PostMapping("/upload-profile-image")
	public ResponseEntity<?> uploadProfileImage(@RequestParam("image") MultipartFile image) {
		try {
			UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			User user = userRepository.findById(userDetails.getId())
					.orElseThrow(() -> new ResourceNotFoundException("User not found"));

			// Delete any previous profile image
			if (user.getProfileImageUrl() != null) {
				deleteProfileImage(user.getProfileImageUrl());
			}

			// Generate a unique filename for the new image
			String filename = UUID.randomUUID().toString();
			String extension = FilenameUtils.getExtension(image.getOriginalFilename());
			String path = PROFILE_IMAGES_DIR + filename + "." + extension;

			// Save the new image
			byte[] bytes = image.getBytes();
			Path filePath = Paths.get(path);
			Files.write(filePath, bytes);

			// Update the user's profile image URL
			user.setProfileImageUrl(path);
			userRepository.save(user);

			return ResponseEntity.ok().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading profile image.");
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
	}

	@GetMapping("/profile-image")
	public ResponseEntity<byte[]> getProfileImage() {
		try {
			UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			User user = userRepository.findById(userDetails.getId())
					.orElseThrow(() -> new ResourceNotFoundException("User not found"));

			String profileImageUrl = user.getProfileImageUrl();

			if (profileImageUrl == null) {
				throw new ResourceNotFoundException("Profile image not found.");
			}

			byte[] imageBytes = Files.readAllBytes(Paths.get(profileImageUrl));
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.IMAGE_JPEG);
			headers.setCacheControl(CacheControl.noCache().getHeaderValue());
			headers.setPragma("no-cache");
			headers.setExpires(0);
			return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving profile image.".getBytes());
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage().getBytes());
		}
	}


//	@GetMapping("/profiles-image")
//	public ResponseEntity<String> getProfileImageUrl() {
//		try {
//			UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//			User user = userRepository.findById(userDetails.getId())
//					.orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//			String profileImageUrl = user.getProfileImageUrl();
//
//			if (profileImageUrl == null) {
//				throw new ResourceNotFoundException("Profile image not found.");
//			}
//
//			HttpHeaders headers = new HttpHeaders();
//			headers.setContentType(MediaType.TEXT_PLAIN);
//			return new ResponseEntity<>(profileImageUrl, headers, HttpStatus.OK);
//		} catch (ResourceNotFoundException e) {
//			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
//		}
//	}


	private void deleteProfileImage(String profileImageUrl) throws IOException {
		Path filePath = Paths.get(profileImageUrl);
		Files.deleteIfExists(filePath);
	}





	@GetMapping("/suggested")
	public ResponseEntity<?> getSuggestedFriends() {
		try {
			// Get the authenticated user
			UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			User currentUser = userRepository.findById(userDetails.getId())
					.orElseThrow(() -> new ResourceNotFoundException("User not found"));

			// Find all friends of the authenticated user
			Set<User> friends = new HashSet<>();
			for (Friendship friendship : currentUser.getFriendships()) {
				if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
					friends.add(friendship.getFriend());
				}
			}

			// Find all users that are not already friends with the authenticated user
			List<UserSummary> suggestedFriends = new ArrayList<>();
			for (User user : userRepository.findAll()) {
				if (!user.equals(currentUser) && !friends.contains(user)) {
					UserSummary summary = new UserSummary();
					summary.setId(user.getId());
					summary.setFullName(user.getFullName());
					summary.setLocation(user.getLocation());
					summary.setGender(user.getGender());
					summary.setEmail(user.getEmail());
					summary.setProfileImageUrl(user.getProfileImageUrl());
					summary.setBio(user.getBio());
					summary.setPhoneNumber(user.getPhoneNumber());
					suggestedFriends.add(summary);
				}
			}

			// Sort the suggested friends by the number of mutual friends they have with the authenticated user
//			suggestedFriends.sort((u1, u2) -> {
//				Set<User> friends1 = new HashSet<>(friends);
//				Set<User> friends2 = new HashSet<>(friends);
//				friends1.retainAll(u1.getFriendships());
//				friends2.retainAll(u2.getFriendships());
//				return Integer.compare(friends2.size(), friends1.size());
//			});

			return ResponseEntity.ok(suggestedFriends);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving suggested friends.");
		}
	}

	@GetMapping("/users/mutual-friends")
	public ResponseEntity<?> getMutualFriends() {
		try {
			UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			User currentUser = userRepository.findById(userDetails.getId())
					.orElseThrow(() -> new ResourceNotFoundException("User not found"));

			Set<UserSummary> mutualFriends = new HashSet<>();

			for (Friendship friend1 : currentUser.getFriendships()) {
				if (friend1.getStatus() == FriendshipStatus.ACCEPTED) {
					User user1 = friend1.getFriend();

					for (Friendship friend2 : user1.getFriendships()) {
						if (friend2.getStatus() == FriendshipStatus.ACCEPTED) {
							User user2 = friend2.getFriend();

							if (user2.getId() != currentUser.getId() && !mutualFriends.contains(user2)) {
								Set<User> user1Friends = user1.getFriendships().stream()
										.filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
										.map(Friendship::getFriend)
										.collect(Collectors.toSet());

								if (user1Friends.contains(currentUser) && user1Friends.contains(user2)) {
									UserSummary summary = new UserSummary();
									summary.setId(user2.getId());
									summary.setFullName(user2.getFullName());
									summary.setLocation(user2.getLocation());
									summary.setGender(user2.getGender());
									summary.setEmail(user2.getEmail());
									summary.setProfileImageUrl(user2.getProfileImageUrl());
									summary.setBio(user2.getBio());
									summary.setPhoneNumber(user2.getPhoneNumber());
									mutualFriends.add(summary);
								}
							}
						}
					}
				}
			}

			return ResponseEntity.ok(mutualFriends);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving mutual friends.");
		}
	}



	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);
		
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();		
		

		return ResponseEntity.ok(new JwtResponse(jwt, 
												 userDetails.getId(), 
												 userDetails.getUsername(), 
												 userDetails.getEmail()
												));
	}


	@PostMapping("/add-friend/{id}")
	public ResponseEntity<?> sendsFriendRequest(@PathVariable Long id) {
		UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Long currentUserId = userDetails.getId();

		User currentUser = userRepository.findById(currentUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

		User friend = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

		Optional<Friendship> existingFriendship = friendshipRepository.findByUserAndFriend(currentUser, friend);
		if (existingFriendship.isPresent() && existingFriendship.get().getStatus() == FriendshipStatus.PENDING) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("A friend request has already been sent from this user.");
		}

		try {
			friendshipService.sendFriendRequest(currentUser, friend);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		}
	}

	
	@Autowired
	private FriendshipRepository friendshipRepository;
	@PostMapping("/friendship/accept/{userId}")
	public ResponseEntity<?> acceptFriendRequest(@PathVariable Long userId) {
	    try {
	        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	        User recipient = userRepository.findById(userDetails.getId())
	                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with id: " + userDetails.getId()));
	        User sender = userRepository.findById(userId)
	                .orElseThrow(() -> new IllegalArgumentException("Invalid user id: " + userId));

	        Friendship friendship = friendshipRepository.findByUserAndFriend(sender, recipient)
	                .orElseThrow(() -> new IllegalArgumentException("Friendship request not found"));

	        friendship.setStatus(FriendshipStatus.ACCEPTED);
	        friendship.setCreatedAt(LocalDateTime.now());
	        friendshipRepository.save(friendship);

	        // create and save reverse friendship
	        Friendship reverseFriendship = new Friendship(recipient, sender, FriendshipStatus.ACCEPTED);
	        reverseFriendship.setCreatedAt(LocalDateTime.now());
	        friendshipRepository.save(reverseFriendship);

	        return ResponseEntity.ok(friendship);
	    } catch (UsernameNotFoundException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	    } catch (IllegalArgumentException e) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error accepting friend request.");
	    }
	}



	@DeleteMapping("/remove-friend/{id}")
	public ResponseEntity<?> removeFriendship(@PathVariable Long id) {
	    try {
	        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	        User currentUser = userRepository.findById(userDetails.getId())
	                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with id: " + userDetails.getId()));
	        User friend = userRepository.findById(id)
	                .orElseThrow(() -> new IllegalArgumentException("Invalid user id: " + id));

	        Friendship friendship = friendshipRepository.findByUserAndFriend(currentUser, friend)
	                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

	        Friendship reverseFriendship = friendshipRepository.findByUserAndFriend(friend, currentUser)
	                .orElse(null);

	        if (reverseFriendship != null) {
	            friendshipRepository.delete(reverseFriendship);
	        }

	        friendshipRepository.delete(friendship);

	        return ResponseEntity.ok().build();
	    } catch (UsernameNotFoundException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	    } catch (IllegalArgumentException e) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error removing friendship.");
	    }
	}


	


	@GetMapping("/users/{id}/friends")
	public ResponseEntity<?> getAllFriends(@PathVariable Long id) {
	    try {
	        User user = userRepository.findById(id)
	                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

	        Set<Friendship> friendships = user.getFriendships();
	        Set<UserSummary> friends = new HashSet<>();

	        for (Friendship friendship : friendships) {
	            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
	                User friend = friendship.getFriend();
	                UserSummary summary = new UserSummary();
	                summary.setId(friend.getId());
	                summary.setFullName(friend.getFullName());
	                summary.setLocation(friend.getLocation());
	                summary.setGender(friend.getGender());
	                summary.setEmail(friend.getEmail());
	                summary.setProfileImageUrl(friend.getProfileImageUrl());
	                summary.setBio(friend.getBio());
	                summary.setPhoneNumber(friend.getPhoneNumber());
	                friends.add(summary);
	            }
	        }

	        return ResponseEntity.ok(friends);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user's friends.");
	    }
	}

	
	
	@GetMapping("/users/friends")
	public ResponseEntity<?> getAllFriends() {
	    try {
	        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//	        System.out.println(userDetails.getUsername());
	        User user = userRepository.findById(userDetails.getId())
	                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

	        Set<Friendship> friendships = user.getFriendships();
	        Set<UserSummary> friends = new HashSet<>();

	        for (Friendship friendship : friendships) {
	            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
	                User friend = friendship.getFriend();
	                UserSummary summary = new UserSummary();
	                summary.setId(friend.getId());
	                summary.setFullName(friend.getFullName());
	                summary.setLocation(friend.getLocation());
	                summary.setGender(friend.getGender());
	                summary.setEmail(friend.getEmail());
	                summary.setProfileImageUrl(friend.getProfileImageUrl());
	                summary.setBio(friend.getBio());
	                friends.add(summary);
	            }
	        }

	        return ResponseEntity.ok(friends);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user's friends.");
	    }
	}

	
	@GetMapping("/users/pending-friends")
	public ResponseEntity<?> getPendingFriends() {
	    try {
	        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	        User user = userRepository.findById(userDetails.getId())
	                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

	        Set<Friendship> friendships = user.getFriendships();
	        List<Map<String, Object>> pendingFriends = new ArrayList<>();

	        for (Friendship friendship : friendships) {
	            if (friendship.getStatus() == FriendshipStatus.PENDING && friendship.getFriend() != user) {
	                User friend = friendship.getFriend();
	                Map<String, Object> friendInfo = new HashMap<>();
	                friendInfo.put("id", friend.getId());
	                friendInfo.put("fullName", friend.getFullName());
	                friendInfo.put("location", friend.getLocation());
	                friendInfo.put("gender", friend.getGender());
	                friendInfo.put("email", friend.getEmail());
	                friendInfo.put("profileImageUrl", friend.getProfileImageUrl());
	                friendInfo.put("bio", friend.getBio());
	                pendingFriends.add(friendInfo);
	            }
	        }

	        return ResponseEntity.ok(pendingFriends);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user's pending friends.");
	    }
	}


	
	@GetMapping("/friendship/pending-requests")
	public ResponseEntity<?> getPendingFriendRequests() {
	    try {
	        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	        User recipient = userRepository.findById(userDetails.getId())
	                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with id: " + userDetails.getId()));

	        List<Friendship> pendingRequests = friendshipRepository.findByFriendAndStatus(recipient, FriendshipStatus.PENDING);
	        List<Map<String, Object>> requestDetails = new ArrayList<>();

	        for (Friendship friendship : pendingRequests) {
	            Map<String, Object> details = new HashMap<>();
	            User friend = friendship.getUser();

	            details.put("id", friend.getId());
	            details.put("fullName", friend.getFullName());
	            details.put("location", friend.getLocation());
	            details.put("gender", friend.getGender());
	            details.put("email", friend.getEmail());
	            details.put("profileimageurl", friend.getProfileImageUrl());
	            details.put("bio", friend.getBio());

	            requestDetails.add(details);
	        }

	        return ResponseEntity.ok(requestDetails);
	    } catch (UsernameNotFoundException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving pending friend requests.");
	    }
	}
	


	@GetMapping("/users/search")
	public ResponseEntity<?> searchUsers(@RequestParam String query) {
	    try {
	        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	        List<User> users = userRepository.findUsersByQueryAndNotFriends(query, userDetails.getId());
	        List<Map<String, Object>> userMaps = users.stream().map(u -> {
	            Map<String, Object> userMap = new HashMap<>();
	            userMap.put("id", u.getId());
	            userMap.put("username", u.getUsername());
	            userMap.put("fullName", u.getFullName());
	            userMap.put("location", u.getLocation());
	            userMap.put("gender", u.getGender());
	            userMap.put("email", u.getEmail());
	            userMap.put("profileimageurl", u.getProfileImageUrl());
	            userMap.put("bio", u.getBio());
	            return userMap;
	        }).collect(Collectors.toList());
	        return ResponseEntity.ok(userMaps);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error searching for users.");
	    }
	}

	



	
	@PutMapping("/update/{id}")
	public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest userUpdateRequest) {
	    
	    User user = userRepository.findById(id).orElse(null);
	    if (user == null) {
	        return ResponseEntity.notFound().build();
	    }

	    if (userUpdateRequest.getEmail() != null && !userUpdateRequest.getEmail().isEmpty()) {
	        user.setEmail(userUpdateRequest.getEmail());
	    }

	    if (userUpdateRequest.getPhoneNumber() != null) {
	        user.setPhoneNumber(userUpdateRequest.getPhoneNumber());
	    }

	    if (userUpdateRequest.getLocation() != null && !userUpdateRequest.getLocation().isEmpty()) {
	        user.setLocation(userUpdateRequest.getLocation());
	    }

	    if (userUpdateRequest.getGender() != null && !userUpdateRequest.getGender().isEmpty()) {
	        user.setGender(userUpdateRequest.getGender());
	    }

	    if (userUpdateRequest.getFullName() != null && !userUpdateRequest.getFullName().isEmpty()) {
	        user.setFullName(userUpdateRequest.getFullName());
	    }

	    if (userUpdateRequest.getBio() != null && !userUpdateRequest.getBio().isEmpty()) {
	        user.setBio(userUpdateRequest.getBio());
	    }

	    if (userUpdateRequest.getUsername() != null && !userUpdateRequest.getUsername().isEmpty()) {
	        user.setUsername(userUpdateRequest.getUsername());
	    }

	    User updatedUser = userRepository.save(user);

	    return ResponseEntity.ok().body(updatedUser);
	}




	@PutMapping("/decline-friend-request/{id}")
	public ResponseEntity<?> declineFriendRequest(@PathVariable Long id, Authentication authentication) {
	    User currentUser = (User) authentication.getPrincipal();
	    User friend = userRepository.findById(id)
	            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
	    friendshipService.blockUser(currentUser, friend);
	    return ResponseEntity.ok().build();
	}
		
		
	
//		 @GetMapping("/user/{id}")
//		    public ResponseEntity<User> getUserById(@PathVariable Long id) throws AttributeNotFoundException {
//		        User user = userRepository.findById(id)
//		                .orElseThrow(() -> new AttributeNotFoundException("User not found with id: " + id));
//		        // Set the password field to null before returning the user
//		        user.setPassword(null);
//		        return new ResponseEntity<User>(user, HttpStatus.OK);
//		    }

//	@GetMapping("/users")
//	public ResponseEntity<List<User>> getAllUsers() {
//		List<User> users = userRepository.findAll();
//		for (User user : users) {
//			user.setPassword(null);
//		}
//		return new ResponseEntity<>(users, HttpStatus.OK);
//	}











	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Username is already taken!"));
		}

		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Email is already in use!"));
		}

		// Create new user's account
		User user = new User(signUpRequest.getUsername(), 
							 signUpRequest.getEmail(),
							 encoder.encode(signUpRequest.getPassword()));

		

		

		user.setUpdatedAt(LocalDateTime.now());
		userRepository.save(user);

		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}
}



//	@GetMapping("/users/{id}/friends")
//	public ResponseEntity<?> getAllFriends(@PathVariable Long id) {
//	    try {
//	        User user = userRepository.findById(id)
//	                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//	        Set<Friendship> friendships = user.getFriendships();
//	        Set<UserSummary> friends = new HashSet<>();
//
//	        for (Friendship friendship : friendships) {
//	            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
//	                User friend = friendship.getFriend();
//	                UserSummary summary = new UserSummary();
//	                summary.setId(friend.getId());
//	                summary.setFullName(friend.getFullName());
//	                summary.setLocation(friend.getLocation());
//	                summary.setGender(friend.getGender());
//	                summary.setEmail(friend.getEmail());
//	                summary.setProfileImageUrl(friend.getProfileImageUrl());
//	                summary.setBio(friend.getBio());
//	                friends.add(summary);
//	            }
//	        }
//
//	        return ResponseEntity.ok(friends);
//	    } catch (Exception e) {
//	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user's friends.");
//	    }
//	}
//



//	@PostMapping("/friendship/accept/{id}")
//	public ResponseEntity<?> acceptFriendRequest(@PathVariable Long id) {
//	    try {
//	        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//	        User user = userRepository.findById(userDetails.getId())
//	                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with id: " + userDetails.getId()));
//	        User friend = userRepository.findById(id)
//	                .orElseThrow(() -> new IllegalArgumentException("Invalid friend id: " + id));
//
//	        Friendship friendship = friendshipService.acceptFriendRequest(user, friend);
//
//	        if (friendship == null) {
//	            return ResponseEntity.badRequest().body("Friendship request not found");
//	        }
//
//	        return ResponseEntity.ok(friendship);
//	    } catch (UsernameNotFoundException e) {
//	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//	    } catch (IllegalArgumentException e) {
//	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
//	    } catch (Exception e) {
//	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error accepting friend request.");
//	    }
//	}


//@PostMapping("/friendship/remove/{friendId}")
//public ResponseEntity<?> removeFriendship(@PathVariable Long friendId) {
//    try {
//        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        User currentUser = userRepository.findById(userDetails.getId())
//                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with id: " + userDetails.getId()));
//        User friendUser = userRepository.findById(friendId)
//                .orElseThrow(() -> new IllegalArgumentException("Invalid user id: " + friendId));
//
//        Friendship friendship1 = friendshipRepository.findByUserAndFriend(currentUser, friendUser)
//                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));
//
//        Friendship friendship2 = friendshipRepository.findByUserAndFriend(friendUser, currentUser)
//                .orElseThrow(() -> new IllegalArgumentException("Reverse friendship not found"));
//
//        friendshipRepository.delete(friendship1);
//        friendshipRepository.delete(friendship2);
//
//        return ResponseEntity.ok("Friendship removed successfully.");
//    } catch (UsernameNotFoundException e) {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//    } catch (IllegalArgumentException e) {
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
//    } catch (Exception e) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error removing friendship.");
//    }
//}

//@PostMapping("/send-friend-request-notification/{id}")
//public ResponseEntity<?> sendFriendRequestNotification(@PathVariable Long id) {
//    UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//    Long currentUserId = userDetails.getId();
//
//    User currentUser = userRepository.findById(currentUserId)
//            .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
//
//    User friend = userRepository.findById(id)
//            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
//
//    String notificationMessage = currentUser.getUsername() + " sent you a friend request.";
//    NotificationService.sendNotification(friend, notificationMessage);
//
//    return ResponseEntity.ok().build();
//}


//@PutMapping("/accept-friend-request/{id}")
//public ResponseEntity<?> acceptFriendRequest(@PathVariable Long id) {
//    UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//    Long currentUserId = userDetails.getId();
//
//    User currentUser = userRepository.findById(currentUserId)
//            .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
//
//    User friend = userRepository.findById(id)
//            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
//
//    friendshipService.acceptFriendRequest(friend, currentUser);
//
//    return ResponseEntity.ok().build();
//}


//@PutMapping("/accept-friend-request/{id}")
//public ResponseEntity<?> acceptFriendRequest(@PathVariable Long id, Authentication authentication) {
//    User currentUser = (User) authentication.getPrincipal();
//    User friend = userRepository.findById(id)
//            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
//    friendshipService.acceptFriendRequest(currentUser, friend);
//    return ResponseEntity.ok().build();
//}


//@GetMapping("/users/search")
//public ResponseEntity<?> searchUsers(@RequestParam String query) {
//    try {
//        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        User user = userRepository.findById(userDetails.getId())
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        List<Map<String, Object>> users = userRepository.findUsersByQuery(query, user.getId())
//                .stream()
//                .map(u -> {
//                    Map<String, Object> userMap = new HashMap<>();
//                    userMap.put("id", u.getId());
//                    userMap.put("username", u.getUsername());
//                    userMap.put("fullName", u.getFullName());
//                    userMap.put("location", u.getLocation());
//                    userMap.put("gender", u.getGender());
//                    userMap.put("email", u.getEmail());
//                    userMap.put("profileimageurl", u.getProfileImageUrl());
//                    userMap.put("bio", u.getBio());
//                    return userMap;
//                })
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok(users);
//    } catch (Exception e) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error searching for users.");
//    }
//}
//@GetMapping("/users/{id}/friends")
//public ResponseEntity<?> getAllFriends(@PathVariable Long id) {
//    try {
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        Set<Friendship> friendships = user.getFriendships();
//        Set<User> friends = new HashSet<>();
//
//        for (Friendship friendship : friendships) {
//            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
//                friends.add(friendship.getFriend());
//            }
//        }
//
//        return ResponseEntity.ok(friends);
//    } catch (Exception e) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user's friends.");
//    }
//}



//@DeleteMapping("/remove-friend/{id}")
//public ResponseEntity<?> removeFriendship(@PathVariable Long id) {
//    UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//    Long currentUserId = userDetails.getId();
//
//    User currentUser = userRepository.findById(currentUserId)
//            .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
//
//    User friend = userRepository.findById(id)
//            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
//
//    friendshipService.removeFriendship(currentUser, friend);
//
//    return ResponseEntity.ok().build();
//}
//@PostMapping("/friendship/accept/{id}")
//public ResponseEntity<?> acceptFriendRequest(@PathVariable Long id) {
//    try {
//        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        User user = userRepository.findById(userDetails.getId())
//                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with id: " + userDetails.getId()));
//        User friend = userRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Invalid friend id: " + id));
//
//        Friendship friendship = friendshipService.acceptFriendRequest(user, friend);
//
//        if (friendship == null) {
//            return ResponseEntity.badRequest().body("Friendship request not found");
//        }
//
//        return ResponseEntity.ok(friendship);
//    } catch (UsernameNotFoundException e) {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//    } catch (IllegalArgumentException e) {
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
//    } catch (Exception e) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error accepting friend request.");
//    }
//}
