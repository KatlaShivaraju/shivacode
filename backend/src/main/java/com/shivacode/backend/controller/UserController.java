
package com.shivacode.backend.controller;

import com.shivacode.backend.dto.UserProfileResponse;
import com.shivacode.backend.service.UserProfileService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            Authentication authentication
    ) {
        UserProfileResponse profile =
                userProfileService.getProfile(authentication.getName());

        return ResponseEntity.ok(profile);
    }
}
