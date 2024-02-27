package com.tekhive.spring.security.postgresql.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table( name = "users",
uniqueConstraints = {
@UniqueConstraint(columnNames = "username"),
@UniqueConstraint(columnNames = "email")
})
public class User {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
@NotBlank
@Size(max = 20)
private String username;

@NotBlank
@Size(max = 50)
@Email
private String email;

@NotBlank
@Size(max = 120)
private String password;

private String gender;

private String location;

@Column(name = "phone_number")
private Long phoneNumber;

@Column(name = "profile_image_url")
private String profileImageUrl;

@Column(name = "full_name")
private String fullName;

@Column(name = "created_at")
@NotNull
private LocalDateTime createdAt;

@Column(name = "updated_at")
private LocalDateTime updatedAt = LocalDateTime.now();


@Column(name = "is_deleted")
private boolean isDeleted = false;

private String bio;

@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
@JsonBackReference

private Set<Friendship> friendships = new HashSet<>();



public User() {
}

public User(String username, String email, String password) {
	this.username = username;
	this.email = email;
	this.password = password;
}

public Long getId() {
	return id;
}

public void setId(Long id) {
	this.id = id;
}

public String getUsername() {
	return username;
}

public void setUsername(String username) {
	this.username = username;
}

public String getEmail() {
	return email;
}

public void setEmail(String email) {
	this.email = email;
}

public String getPassword() {
	return password;
}

public void setPassword(String password) {
	this.password = password;
}

public String getGender() {
	return gender;
}

public void setGender(String gender) {
	this.gender = gender;
}

public String getLocation() {
	return location;
}

public void setLocation(String location) {
	this.location = location;
}

public Long getPhoneNumber() {
	return phoneNumber;
}

public void setPhoneNumber(Long phoneNumber) {
	this.phoneNumber = phoneNumber;
}

public String getProfileImageUrl() {
	return profileImageUrl;
}

public void setProfileImageUrl(String profileImageUrl) {
	this.profileImageUrl = profileImageUrl;
}

public String getFullName() {
	return fullName;
}

public void setFullName(String fullName) {
	this.fullName = fullName;
}

public String getBio() {
    return bio;
}

public void setBio(String bio) {
    this.bio = bio;
}


public LocalDateTime getCreatedAt() {
	return createdAt;
}

public void setCreatedAt(LocalDateTime createdAt) {
	this.createdAt = createdAt;
}

public LocalDateTime getUpdatedAt() {
	return updatedAt;
}

public void setUpdatedAt(LocalDateTime updatedAt) {
	this.updatedAt = updatedAt;
}

public boolean isDeleted() {
	return isDeleted;
}

public void setDeleted(boolean isDeleted) {
	this.isDeleted = isDeleted;
}

@JsonBackReference
public Set<Friendship> getFriendships() {
    return friendships;
}

public void setFriendships(Set<Friendship> friendships) {
this.friendships = friendships;
}
@
JsonBackReference
public void addFriendship(Friendship friendship) {
    this.friendships.add(friendship);
}

public void removeFriendship(Friendship friendship) {
    this.friendships.remove(friendship);
}

@PrePersist
protected void onCreate() {
	this.createdAt = LocalDateTime.now();
}

@PreUpdate
protected void onUpdate() {
	this.updatedAt = LocalDateTime.now();
}

@PreRemove
protected void onDelete() {
	this.isDeleted = true;
}

}

