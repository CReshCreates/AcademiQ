package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Request.Project.ProjectCreationRequest;
import com.softwareprojectmanagement.DTO.Response.Project.*;
import com.softwareprojectmanagement.Services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping("students/getSubjects")
    public ResponseEntity<List<AvailableSubjectsForProjectCreation>> getSubjects(Authentication authentication) {
        List<AvailableSubjectsForProjectCreation> subjects = projectService.getSubjects(authentication.getName());
        return ResponseEntity.status(HttpStatus.OK).body(subjects);
    }

    @GetMapping("students/getSupervisor/{programmeSubjectId}")
    public ResponseEntity<List<AvailableSupervisor>> getSupervisor(@PathVariable Integer programmeSubjectId) {
        return ResponseEntity.ok().body(projectService.getSupervisors(programmeSubjectId));
    }

    @PostMapping(value="students/createProject", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreatedProjectResponse> createNewProject(@ModelAttribute ProjectCreationRequest projectCreationRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.getCreatedProject(projectCreationRequest));
    }

}
