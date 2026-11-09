package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Request.LoginAndRegistration.LoginRequest;
import com.softwareprojectmanagement.DTO.Request.LoginAndRegistration.RegistrationRequest;
import com.softwareprojectmanagement.DTO.Response.LoginResponse;
import com.softwareprojectmanagement.Exceptions.AuthenticationFailedException;
import com.softwareprojectmanagement.Exceptions.EmailAlreadyExistsException;
import com.softwareprojectmanagement.Exceptions.SectionNotCurrentlyAvailableException;
import com.softwareprojectmanagement.Models.MyUserDetails;
import com.softwareprojectmanagement.Models.Programme;
import com.softwareprojectmanagement.Models.Section;
import com.softwareprojectmanagement.Models.User;
import com.softwareprojectmanagement.Repository.ProgrammeRepository;
import com.softwareprojectmanagement.Repository.SectionRepository;
import com.softwareprojectmanagement.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SectionRepository sectionRepository;
    private final ProgrammeRepository programmeRepository;


    public String registration(RegistrationRequest registrationRequest) {

        if(userRepository.existsByEmail(registrationRequest.getEmail())){
            throw new EmailAlreadyExistsException("User with this email is already registered!");
        }

        Section section = sectionRepository.findIdByName(registrationRequest.getSection(), registrationRequest.getBatchName());

        Programme programme = programmeRepository.findProgrammeById(registrationRequest.getProgrammeId());

        if(section == null){
            throw new SectionNotCurrentlyAvailableException("There are no sections currently.");
        }
        User user = new User();

        user.setFullName(registrationRequest.getFullName());
        user.setEmail(registrationRequest.getEmail());
        user.setPasswordHash(new BCryptPasswordEncoder(12).encode(registrationRequest.getPassword()));
        user.setRole("STUDENT");

        user.setSection(section);
        user.setProgramme(programme);

        return userRepository.save(user).getEmail();
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();

        if(!authentication.isAuthenticated()){
            throw new AuthenticationFailedException("Authentication Failed!");
        }

        String role = authentication.getAuthorities()
                .stream()
                .findFirst()
                .get()
                .getAuthority();

        String token = jwtService.generateToken(userDetails.getUsername(), role);
        return new LoginResponse(userDetails.getFullName(), userDetails.getUsername(), role, token);
    }
}
