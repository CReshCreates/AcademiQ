package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Request.LoginAndRegistration.LoginRequest;
import com.softwareprojectmanagement.DTO.Request.LoginAndRegistration.RegistrationRequest;
import com.softwareprojectmanagement.DTO.Response.LoginResponse;
import com.softwareprojectmanagement.Services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> userRegistration(@Valid @RequestBody RegistrationRequest registrationRequest) {
        String email = userService.registration(registrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(email);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = userService.login(loginRequest);
        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }
}
