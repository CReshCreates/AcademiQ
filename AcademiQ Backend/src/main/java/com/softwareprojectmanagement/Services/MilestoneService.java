
package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Response.Milestone.MilestoneResponse;

import com.softwareprojectmanagement.Models.Milestone;
import com.softwareprojectmanagement.Models.Project;
import com.softwareprojectmanagement.Models.ProjectMember;
import com.softwareprojectmanagement.Models.User;

import com.softwareprojectmanagement.Repository.MilestoneRepository;
import com.softwareprojectmanagement.Repository.ProjectMemberRepository;
import com.softwareprojectmanagement.Repository.ProjectRepository;
import com.softwareprojectmanagement.Repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    // GET ALL MILESTONES FOR A PROJECT
    @Transactional(readOnly = true)
    public List<MilestoneResponse> getProjectMilestones(
            Integer projectId
    ) {

        // Step 1: Get authenticated user.
        User currentUser = getCurrentUser();

        // Step 2: Verify project exists.
        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Project not found."
                        )
                );

        // Step 3: Verify project access.
        validateProjectAccess(project, currentUser);

        // Step 4: Retrieve project milestones.
        List<Milestone> milestones =
                milestoneRepository.findMilestonesByProjectId(
                        projectId
                );

        // Step 5: Convert entities into DTOs.
        return milestones.stream()
                .map(this::mapToMilestoneResponse)
                .toList();
    }
    // GET AUTHENTICATED USER

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new AccessDeniedException(
                    "Authentication is required."
            );
        }

        User currentUser =
                userRepository.findByEmail(
                        authentication.getName()
                );

        if (currentUser == null) {
            throw new UsernameNotFoundException(
                    "Authenticated user not found."
            );
        }

        return currentUser;
    }

    // VALIDATE PROJECT ACCESS
    private void validateProjectAccess(
            Project project,
            User currentUser
    ) {

        Integer currentUserId = currentUser.getUserId();

        // Project creator can view milestones.
        boolean isProjectCreator =
                project.getUser() != null
                        && currentUserId.equals(
                        project.getUser().getUserId()
                );

        // Assigned supervisor can view milestones.
        boolean isAssignedSupervisor =
                project.getSupervisor() != null
                        && currentUserId.equals(
                        project.getSupervisor().getUserId()
                )
                        && "SUPERVISOR".equalsIgnoreCase(
                        currentUser.getRole()
                );

        // Accepted project members can view milestones.
        boolean isProjectMember =
                projectMemberRepository
                        .findMembersByProjectId(
                                project.getProject_id()
                        )
                        .stream()
                        .anyMatch(member ->
                                member.getUser() != null
                                        && currentUserId.equals(
                                        member.getUser().getUserId()
                                )
                        );

        if (!isProjectCreator
                && !isAssignedSupervisor
                && !isProjectMember) {

            throw new AccessDeniedException(
                    "You are not authorized to view "
                            + "this project's milestones."
            );
        }
    }

    // MAP MILESTONE TO RESPONSE DTO

    private MilestoneResponse mapToMilestoneResponse(
            Milestone milestone
    ) {

        return new MilestoneResponse(
                milestone.getMilestoneId(),
                milestone.getTitle(),
                milestone.getDescription(),
                milestone.getDeadline(),
                milestone.getStatus(),
                milestone.getProgress()
        );
    }
}
