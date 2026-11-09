
package com.softwareprojectmanagement.Config;

import com.softwareprojectmanagement.Models.Project;
import com.softwareprojectmanagement.Models.User;

import com.softwareprojectmanagement.Models.Workspace;
import com.softwareprojectmanagement.Models.WorkspaceMembership;
import com.softwareprojectmanagement.Repository.ProjectMemberRepository;
import com.softwareprojectmanagement.Repository.ProjectRepository;
import com.softwareprojectmanagement.Repository.UserRepository;

import com.softwareprojectmanagement.Repository.WorkspaceMembershipRepository;
import com.softwareprojectmanagement.Services.JwtService;
import com.softwareprojectmanagement.Services.MyUserDetailsService;

import com.softwareprojectmanagement.Services.ProjectChatAuthorizationService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor
        implements ChannelInterceptor {

    private static final String SUBSCRIBE_PREFIX =
            "/topic/projects/";

    private static final String SEND_PREFIX =
            "/app/chat.send/";

    private final JwtService jwtService;
    private final MyUserDetailsService userDetailsService;

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ProjectChatAuthorizationService projectChatAuthorizationService;

    // INTERCEPT INCOMING STOMP FRAMES
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                org.springframework.messaging.support.MessageHeaderAccessor
                        .getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command != StompCommand.CONNECT
                && command != StompCommand.SUBSCRIBE
                && command != StompCommand.SEND) {
            return message;
        }

        String destination = accessor.getDestination();
        log.info("CHAT DEBUG received command={} destination={}", command, destination);
        try {
            if (command == StompCommand.CONNECT) {
                authenticateConnection(accessor);
            } else if (command == StompCommand.SUBSCRIBE) {
                authorizeDestination(accessor, SUBSCRIBE_PREFIX);
            } else {
                authorizeDestination(accessor, SEND_PREFIX);
            }
            log.info("CHAT DEBUG accepted command={} destination={}", command, destination);
            return message;
        } catch (Exception exception) {
            log.error("CHAT DEBUG rejected command={} destination={} reason={}",
                    command, destination, exception.getMessage(), exception);
            throw exception;
        }
    }

    // AUTHENTICATE STOMP CONNECT
    private void authenticateConnection(
            StompHeaderAccessor accessor
    ) {

        String authorizationHeader =
                accessor.getFirstNativeHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            throw new BadCredentialsException(
                    "JWT token is required for WebSocket connection."
            );
        }

        String token = authorizationHeader.substring(7);

        try {

            String email =
                    jwtService.extractUserName(token);

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            if (!jwtService.validateToken(token, userDetails)) {

                throw new BadCredentialsException(
                        "Invalid or expired WebSocket JWT."
                );
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            accessor.setUser(authentication);

        } catch (Exception exception) {

            throw new BadCredentialsException(
                    "WebSocket authentication failed.",
                    exception
            );
        }
    }

    // AUTHORIZE SEND OR SUBSCRIBE DESTINATION
    private void authorizeDestination(
            StompHeaderAccessor accessor,
            String requiredPrefix
    ) {

        User currentUser = getSessionUser(accessor);

        String destination = accessor.getDestination();

        if (destination == null
                || !destination.startsWith(requiredPrefix)) {

            throw new AccessDeniedException(
                    "STOMP destination is not permitted."
            );
        }

        String projectIdPart =
                destination.substring(requiredPrefix.length());

        // Require an exact numeric project ID.
        // Reject nested or unexpected destinations.

        if (!projectIdPart.matches("[0-9]+")) {

            throw new AccessDeniedException(
                    "Invalid project chat destination."
            );
        }

        Integer projectId;

        try {

            projectId = Integer.valueOf(projectIdPart);

        } catch (NumberFormatException exception) {

            throw new AccessDeniedException(
                    "Invalid project ID."
            );
        }

        projectChatAuthorizationService.validateProjectAccess(projectId, currentUser);
    }

    // IDENTIFY AUTHENTICATED STOMP SESSION USER
    private User getSessionUser(
            StompHeaderAccessor accessor
    ) {

        if (!(accessor.getUser() instanceof Authentication authentication)
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Authenticated WebSocket session is required."
            );
        }

        User currentUser =
                userRepository.findByEmail(authentication.getName());

        if (currentUser == null) {

            throw new AccessDeniedException(
                    "Authenticated user not found."
            );
        }

        return currentUser;
    }


    // CHECK PROJECT CHAT ACCESS USING WORKSPACE MEMBERSHIP
    private void validateProjectAccess(
            Integer projectId,
            User currentUser
    ) {

        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Project not found or access denied."
                        )
                );

        // 1. Validate the project's workspace.
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

        // 2. Retrieve active workspace memberships.
        java.util.List<WorkspaceMembership> memberships =
                workspaceMembershipRepository.findUserMembershipsByStatus(
                        userId,
                        WorkspaceMembership.MembershipStatus.ACTIVE
                );

        // 3. Require an active membership in this project's workspace.
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

        // 4. Check project-level permissions.
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
                                        member.getUser()
                                                .getUserId()
                                )
                        );

        // 5. Deny access unless both workspace and project-level authorization are satisfied.
        if (!isCreator && !isSupervisor && !isMember) {

            throw new AccessDeniedException(
                    "You are not authorized for this project chat."
            );
        }
    }

}
