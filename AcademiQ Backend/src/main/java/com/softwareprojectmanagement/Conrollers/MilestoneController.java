
package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Response.Milestone.MilestoneResponse;
import com.softwareprojectmanagement.Services.MilestoneService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    // GET ALL MILESTONES FOR A PROJECT
    @GetMapping
    public ResponseEntity<List<MilestoneResponse>> getProjectMilestones(
            @PathVariable Integer projectId
    ) {

        List<MilestoneResponse> milestones =
                milestoneService.getProjectMilestones(projectId);

        return ResponseEntity.ok(milestones);
    }
}
