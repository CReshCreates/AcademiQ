package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.Models.Subject;
import com.softwareprojectmanagement.Services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping("/getSubjects")
    public ResponseEntity<List<Subject>> getSubjects(Authentication authentication) {
        List<Subject> subjects = projectService.getSubjects(authentication.getName());
        return ResponseEntity.status(HttpStatus.OK).body(subjects);
    }
}
