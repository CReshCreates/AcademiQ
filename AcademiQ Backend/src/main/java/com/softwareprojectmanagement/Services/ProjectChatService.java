
package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Request.Chat.ChatMessageRequest;
import com.softwareprojectmanagement.DTO.Response.Chat.ChatMessageResponse;

import com.softwareprojectmanagement.Models.*;

import com.softwareprojectmanagement.Repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final MessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;

    // SAVE A PROJECT CHAT MESSAGE
    @Transactional
    public ChatMessageResponse sendMessage(
            Integer projectId,
            ChatMessageRequest request,
            Principal principal
    ) {

        User currentUser = getAuthenticatedUser(principal);

        Project project = getAuthorizedProject(
                projectId,
                currentUser
        );

        if (request == null
                || request.getContent() == null
                || request.getContent().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message cannot be empty."
            );
        }

        String content = request.getContent().trim();

        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message cannot exceed 2000 characters."
            );
        }

        Message message = new Message();

        message.setProject(project);
        message.setUser(currentUser);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());

        Message savedMessage =
                messageRepository.save(message);

        return mapToResponse(savedMessage);
    }

    // GET PROJECT CHAT HISTORY
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(
            Integer projectId
    ) {

        User currentUser = getAuthenticatedUser();

        getAuthorizedProject(projectId, currentUser);

        return messageRepository
                .findMessagesByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET AUTHENTICATED USER
    private User getAuthenticatedUser() {

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


    // VERIFY PROJECT CHAT ACCESS WITH WORKSPACE MEMBERSHIP
    private Project getAuthorizedProject(
            Integer projectId,
            User currentUser
    ) {

        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Project not found."
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
                workspaceMembershipRepository.findUserMembershipsByStatus(
                        userId,
                        WorkspaceMembership.MembershipStatus.ACTIVE
                );

        WorkspaceMembership membership = memberships.stream()
                .filter(item ->
                        item.getWorkspace().getWorkspaceId()
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

        boolean isProjectCreator =
                role == WorkspaceMembership.WorkspaceRole.STUDENT
                        && project.getUser() != null
                        && userId.equals(
                        project.getUser().getUserId()
                );

        boolean isAssignedSupervisor =
                role == WorkspaceMembership.WorkspaceRole.SUPERVISOR
                        && project.getSupervisor() != null
                        && userId.equals(
                        project.getSupervisor().getUserId()
                );

        boolean isProjectMember =
                role == WorkspaceMembership.WorkspaceRole.STUDENT
                        && projectMemberRepository
                        .findMembersByProjectId(projectId)
                        .stream()
                        .anyMatch(member ->
                                member.getUser() != null
                                        && userId.equals(
                                        member.getUser()
                                                .getUserId()
                                )
                        );

        if (!isProjectCreator
                && !isAssignedSupervisor
                && !isProjectMember) {

            throw new AccessDeniedException(
                    "You are not authorized to access this project chat."
            );
        }

        return project;
    }


    // GET AUTHENTICATED WEBSOCKET USER
    private User getAuthenticatedUser(
            Principal principal
    ) {

        if (!(principal instanceof Authentication authentication)
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new AccessDeniedException(
                    "Authenticated WebSocket session is required."
            );
        }

        User currentUser =
                userRepository.findByEmail(
                        authentication.getName()
                );

        if (currentUser == null) {
            throw new UsernameNotFoundException(
                    "Authenticated WebSocket user not found."
            );
        }

        return currentUser;
    }


    // CONVERT ENTITY TO RESPONSE DTO
    private ChatMessageResponse mapToResponse(
            Message message
    ) {

        User sender = message.getUser();

        return new ChatMessageResponse(
                message.getMessageId(),
                message.getProject().getProject_id(),
                sender.getUserId(),
                sender.getFullName(),
                sender.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
