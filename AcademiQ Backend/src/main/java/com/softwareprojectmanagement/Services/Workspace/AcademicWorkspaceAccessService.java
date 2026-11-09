package com.softwareprojectmanagement.Services.Workspace;

import com.softwareprojectmanagement.Models.Project;
import com.softwareprojectmanagement.Models.User;
import com.softwareprojectmanagement.Models.Workspace;
import com.softwareprojectmanagement.Models.WorkspaceMembership;
import com.softwareprojectmanagement.Repository.WorkspaceMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AcademicWorkspaceAccessService {
    private final WorkspaceMembershipRepository membershipRepository;

    public void requireRole(Project project, User user, WorkspaceMembership.WorkspaceRole requiredRole) {
        Workspace workspace = requireActiveAcademicWorkspace(project);
        boolean allowed = membershipRepository.findUserMembershipsByStatus(
                user.getUserId(), WorkspaceMembership.MembershipStatus.ACTIVE)
                .stream().anyMatch(m -> m.getWorkspace().getWorkspaceId().equals(workspace.getWorkspaceId())
                        && m.getRole() == requiredRole);
        if (!allowed) {
            throw new AccessDeniedException("Active " + requiredRole + " membership in the project's workspace is required.");
        }
    }

    public WorkspaceMembership.WorkspaceRole requireMembership(Project project, User user) {
        Workspace workspace = requireActiveAcademicWorkspace(project);
        return membershipRepository.findUserMembershipsByStatus(
                user.getUserId(), WorkspaceMembership.MembershipStatus.ACTIVE)
                .stream().filter(m -> m.getWorkspace().getWorkspaceId().equals(workspace.getWorkspaceId()))
                .findFirst().orElseThrow(() -> new AccessDeniedException(
                        "Active membership in the project's workspace is required.")).getRole();
    }

    public Workspace requireActiveAcademicWorkspace(Project project) {
        if (project == null || project.getWorkspace() == null ||
                project.getWorkspace().getWorkspaceType() != Workspace.WorkspaceType.ACADEMIC ||
                project.getWorkspace().getStatus() != Workspace.WorkspaceStatus.ACTIVE) {
            throw new AccessDeniedException("An active academic project workspace is required.");
        }
        return project.getWorkspace();
    }
}
