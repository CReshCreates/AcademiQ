
package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Request.Chat.ChatMessageRequest;
import com.softwareprojectmanagement.DTO.Response.Chat.ChatMessageResponse;

import com.softwareprojectmanagement.Models.Message;
import com.softwareprojectmanagement.Models.Project;
import com.softwareprojectmanagement.Models.User;

import com.softwareprojectmanagement.Repository.MessageRepository;
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

    // SAVE A PROJECT CHAT MESSAGE
    @Transactional
    public ChatMessageResponse sendMessage(
            Integer projectId,
            ChatMessageRequest request,
            Principal principal
    ) {

        User currentUser = getAuthenticatedUser();

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

    // VERIFY PROJECT CHAT ACCESS
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

        Integer userId = currentUser.getUserId();

        boolean isProjectCreator =
                "STUDENT".equalsIgnoreCase(
                        currentUser.getRole()
                )
                        && project.getUser() != null
                        && userId.equals(
                        project.getUser().getUserId()
                );

        boolean isAssignedSupervisor =
                "SUPERVISOR".equalsIgnoreCase(
                        currentUser.getRole()
                )
                        && project.getSupervisor() != null
                        && userId.equals(
                        project.getSupervisor().getUserId()
                );

        boolean isProjectMember =
                "STUDENT".equalsIgnoreCase(
                        currentUser.getRole()
                )
                        && projectMemberRepository
                        .findMembersByProjectId(projectId)
                        .stream()
                        .anyMatch(member ->
                                member.getUser() != null
                                        && userId.equals(
                                        member.getUser().getUserId()
                                )
                        );

        if (!isProjectCreator
                && !isAssignedSupervisor
                && !isProjectMember) {

            throw new AccessDeniedException(
                    "You are not authorized to access "
                            + "this project chat."
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
