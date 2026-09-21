package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Response.ProjectMember.ProjectMemberInvitationSearchResponse;
import com.softwareprojectmanagement.Services.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
public class ProjectMemberController {
    private final ProjectMemberService projectMemberService;

    @GetMapping("/projects/{projectId}/search")
    public ResponseEntity<List<ProjectMemberInvitationSearchResponse>> getProjectMemberInvitationSearchResponse(@PathVariable Integer projectId, @RequestParam String query) {
        return ResponseEntity.status(HttpStatus.OK).body(projectMemberService.searchResults(query,  projectId));
    }

}
