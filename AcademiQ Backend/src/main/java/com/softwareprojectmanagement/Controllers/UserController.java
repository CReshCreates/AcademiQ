
package com.softwareprojectmanagement.Controllers;

import com.softwareprojectmanagement.DTO.Request.LoginAndRegistration.LoginRequest;
import com.softwareprojectmanagement.DTO.Response.LoginResponse;
import com.softwareprojectmanagement.Services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ==========================================
    // LEGACY PUBLIC REGISTRATION - DISABLED
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> userRegistration() {

        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(Map.of(
                        "message",
                        "Direct student registration is no longer available. "
                                + "Create a new workspace or join through "
                                + "a workspace invitation."
                ));
    }

    // ==========================================
    // EXISTING LOGIN - UNCHANGED
    // ==========================================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {

        LoginResponse loginResponse =
                userService.login(loginRequest);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(loginResponse);
    }
}
