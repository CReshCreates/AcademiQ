
package com.softwareprojectmanagement.Config;

import com.softwareprojectmanagement.Models.Project;
import com.softwareprojectmanagement.Models.User;

import com.softwareprojectmanagement.Repository.ProjectMemberRepository;
import com.softwareprojectmanagement.Repository.ProjectRepository;
import com.softwareprojectmanagement.Repository.UserRepository;

import com.softwareprojectmanagement.Services.JwtService;
import com.softwareprojectmanagement.Services.MyUserDetailsService;

import lombok.RequiredArgsConstructor;

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

    // INTERCEPT INCOMING STOMP FRAMES
    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(message);

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {

            authenticateConnection(accessor);

        } else if (StompCommand.SUBSCRIBE.equals(command)) {

            authorizeDestination(
                    accessor,
                    SUBSCRIBE_PREFIX
            );

        } else if (StompCommand.SEND.equals(command)) {

            authorizeDestination(
                    accessor,
                    SEND_PREFIX
            );
        }

        return message;
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

        validateProjectAccess(projectId, currentUser);
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

    // CHECK PROJECT CHAT MEMBERSHIP
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

        Integer userId = currentUser.getUserId();

        boolean isCreator =
                "STUDENT".equalsIgnoreCase(currentUser.getRole())
                        && project.getUser() != null
                        && userId.equals(
                        project.getUser().getUserId()
                );

        boolean isSupervisor =
                "SUPERVISOR".equalsIgnoreCase(currentUser.getRole())
                        && project.getSupervisor() != null
                        && userId.equals(
                        project.getSupervisor().getUserId()
                );

        boolean isMember =
                "STUDENT".equalsIgnoreCase(currentUser.getRole())
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
