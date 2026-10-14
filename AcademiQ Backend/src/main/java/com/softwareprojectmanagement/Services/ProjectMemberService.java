
package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Response.ProjectMember.ProjectMemberInvitationSearchResponse;
import com.softwareprojectmanagement.DTO.Response.Team.ProjectTeamResponse;
import com.softwareprojectmanagement.DTO.Response.Team.TeamMemberResponse;

import com.softwareprojectmanagement.Exceptions.NoSuchProjectsException;

import com.softwareprojectmanagement.Models.InvitationStatus;
import com.softwareprojectmanagement.Models.Project;
import com.softwareprojectmanagement.Models.ProjectMember;
import com.softwareprojectmanagement.Models.User;

import com.softwareprojectmanagement.Repository.InvitationRepository;
import com.softwareprojectmanagement.Repository.ProjectMemberRepository;
import com.softwareprojectmanagement.Repository.ProjectRepository;
import com.softwareprojectmanagement.Repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private static final int MAX_PROJECT_MEMBERS = 3;

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final InvitationRepository invitationRepository;


    // ==========================================
    // GET AUTHENTICATED STUDENT
    // ==========================================

    private User getCurrentStudent() {

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

        User currentUser = userRepository
                .findByEmail(authentication.getName());

        if (currentUser == null) {
            throw new UsernameNotFoundException(
                    "Authenticated user not found."
            );
        }

        if (!"STUDENT".equalsIgnoreCase(
                currentUser.getRole()
        )) {
            throw new AccessDeniedException(
                    "Only students can access this operation."
            );
        }

        return currentUser;
    }


    // ==========================================
    // SEARCH ELIGIBLE STUDENTS FOR INVITATION
    // ==========================================

    @Transactional(readOnly = true)
    public List<ProjectMemberInvitationSearchResponse> searchResults(
            String query,
            Integer projectId
    ) {

        User currentUser = getCurrentStudent();

        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new NoSuchProjectsException(
                                "There are currently no projects with id: "
                                        + projectId
                        )
                );

        // Only the project leader can search for
        // students to invite to this project.
        if (project.getUser() == null
                || !currentUser.getUserId().equals(
                project.getUser().getUserId()
        )) {

            throw new AccessDeniedException(
                    "Only the project leader can search "
                            + "for students to invite."
            );
        }

        if (currentUser.getSection() == null) {
            throw new IllegalStateException(
                    "Student section information is missing."
            );
        }

        if (project.getProgrammeSubject() == null
                || project.getProgrammeSubject().getSubject() == null) {
            throw new IllegalStateException(
                    "Project academic subject information is missing."
            );
        }

        Integer sectionId =
                currentUser.getSection().getSectionId();

        Integer subjectId =
                project.getProgrammeSubject()
                        .getSubject()
                        .getSubjectId();

        if (query == null || query.isBlank()) {
            return List.of();
        }

        List<User> users =
                userRepository.searchEligibleStudents(
                        query.trim(),
                        sectionId,
                        subjectId,
                        projectId
                );

        return users.stream()
                .map(user ->
                        new ProjectMemberInvitationSearchResponse(
                                user.getUserId(),
                                user.getFullName(),
                                user.getEmail()
                        )
                )
                .toList();
    }


    // ==========================================
    // GET MY PROJECT TEAMS
    // ==========================================


    @Transactional(readOnly = true)
    public List<ProjectTeamResponse> getMyProjectTeams() {

        User currentUser = getCurrentStudent();

        List<Project> projects =
                projectRepository.findAllProjectsForStudent(
                        currentUser.getUserId()
                );

        return projects.stream()
                .map(project ->
                        mapToProjectTeamResponse(
                                project,
                                currentUser
                        )
                )
                .toList();
    }



    // ==========================================
    // MAP PROJECT TO TEAM RESPONSE
    // ==========================================


    private ProjectTeamResponse mapToProjectTeamResponse(
            Project project,
            User currentUser
    ) {

        // Retrieve accepted project members.
        List<ProjectMember> projectMembers =
                projectMemberRepository.findMembersByProjectId(
                        project.getProject_id()
                );

        List<TeamMemberResponse> members =
                projectMembers.stream()
                        .map(this::mapToTeamMemberResponse)
                        .toList();

        int memberCount = members.size();

        // Count invitations that are still pending.
        long pendingCount =
                invitationRepository.countInvitationsByProjectAndStatus(
                        project.getProject_id(),
                        InvitationStatus.PENDING
                );

        // Calculate remaining team capacity.
        int availableSlots = (int) Math.max(
                0L,
                3L - memberCount - pendingCount
        );

        // Identify the project leader.
        boolean isLeader =
                project.getUser() != null
                        && currentUser.getUserId().equals(
                        project.getUser().getUserId()
                );

        // Only leaders with available slots may invite.
        boolean canInvite =
                isLeader && availableSlots > 0;

        return new ProjectTeamResponse(
                project.getProject_id(),
                project.getTitle(),
                project.getStatus(),
                memberCount,
                MAX_PROJECT_MEMBERS,
                Math.toIntExact(pendingCount),
                availableSlots,
                isLeader,
                canInvite,
                members
        );
    }



    // ==========================================
    // MAP MEMBER TO RESPONSE
    // ==========================================

    private TeamMemberResponse mapToTeamMemberResponse(
            ProjectMember member
    ) {

        User user = member.getUser();

        return new TeamMemberResponse(
                member.getMemberId(),
                user.getUserId(),
                user.getFullName(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
