package com.tekhive.spring.security.postgresql.payload.request;

import org.springframework.web.multipart.MultipartFile;

public class UserUpdateRequest {

	   private String email;

	    private Long phoneNumber;

	    private String location;

	    private String gender;

	    private String profileImageUrl;
	    
	    private String fullName;
	    
	    private String bio;
	    
	    private String username;

 

	    public String getEmail() {
	        return email;
	    }

	    public void setEmail(String email) {
	        this.email = email;
	    }
	    
	    public String getUsername() {
	    	return username;
	    }

	    public void setUsername(String username) {
	    	this.username = username;
	    }

	    public Long getPhoneNumber() {
	        return phoneNumber;
	    }

	    public void setPhoneNumber(Long phoneNumber) {
	        this.phoneNumber = phoneNumber;
	    }

	    public String getLocation() {
	        return location;
	    }

	    public void setLocation(String location) {
	        this.location = location;
	    }

	    public String getGender() {
	        return gender;
	    }

	    public void setGender(String gender) {
	        this.gender = gender;
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
}
