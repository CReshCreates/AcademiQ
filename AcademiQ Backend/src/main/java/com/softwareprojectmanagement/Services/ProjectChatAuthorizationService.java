package com.softwareprojectmanagement.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.softwareprojectmanagement.Models.Project;
import com.softwareprojectmanagement.Models.User;
import com.softwareprojectmanagement.Models.Workspace;
import com.softwareprojectmanagement.Models.WorkspaceMembership;

import com.softwareprojectmanagement.Repository.ProjectRepository;
import com.softwareprojectmanagement.Repository.ProjectMemberRepository;
import com.softwareprojectmanagement.Repository.WorkspaceMembershipRepository;

import org.springframework.security.access.AccessDeniedException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectChatAuthorizationService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;

    @Transactional(readOnly = true)
    public void validateProjectAccess(
            Integer projectId,
            User currentUser
    ) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Project not found or access denied."
                        )
                );

        Workspace workspace = project.getWorkspace();

        if (workspace == null
                || workspace.getWorkspaceType()
                != Workspace.WorkspaceType.ACADEMIC
                || workspace.getStatus()
                != Workspace.WorkspaceStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "Project chat is unavailable for this workspace."
            );
        }

        Integer userId = currentUser.getUserId();

        List<WorkspaceMembership> memberships =
                workspaceMembershipRepository
                        .findUserMembershipsByStatus(
                                userId,
                                WorkspaceMembership.MembershipStatus.ACTIVE
                        );

        WorkspaceMembership membership = memberships.stream()
                .filter(item ->
                        item.getWorkspace()
                                .getWorkspaceId()
                                .equals(workspace.getWorkspaceId())
                )
                .findFirst()
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Active workspace membership is required."
                        )
                );

        WorkspaceMembership.WorkspaceRole role =
                membership.getRole();

        boolean isCreator =
                role == WorkspaceMembership.WorkspaceRole.STUDENT
                        && project.getUser() != null
                        && userId.equals(
                        project.getUser().getUserId()
                );

        boolean isSupervisor =
                role == WorkspaceMembership.WorkspaceRole.SUPERVISOR
                        && project.getSupervisor() != null
                        && userId.equals(
                        project.getSupervisor().getUserId()
                );

        boolean isMember =
                role == WorkspaceMembership.WorkspaceRole.STUDENT
                        && projectMemberRepository
                        .findMembersByProjectId(projectId)
                        .stream()
                        .anyMatch(member ->
                                member.getUser() != null
                                        && userId.equals(
                                        member.getUser().getUserId()
                                )
                        );

        if (!isCreator && !isSupervisor && !isMember) {
            throw new AccessDeniedException(
                    "You are not authorized for this project chat."
            );
        }
    }
}