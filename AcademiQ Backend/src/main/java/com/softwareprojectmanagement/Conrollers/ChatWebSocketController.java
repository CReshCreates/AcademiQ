
package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Request.Chat.ChatMessageRequest;
import com.softwareprojectmanagement.DTO.Response.Chat.ChatMessageResponse;
import com.softwareprojectmanagement.Services.ProjectChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ProjectChatService projectChatService;
    private final SimpMessagingTemplate messagingTemplate;

    // RECEIVE AND BROADCAST PROJECT CHAT MESSAGE
    @MessageMapping("/chat.send/{projectId}")
    public void sendMessage(
            @DestinationVariable Integer projectId,
            @Valid ChatMessageRequest request,
            Principal principal
    ) {

        // Authenticate, authorize and save to MySQL.
        ChatMessageResponse savedMessage =
                projectChatService.sendMessage(
                        projectId,
                        request,
                        principal
                );

        // Publish the saved message to subscribers.
        messagingTemplate.convertAndSend(
                "/topic/projects/" + projectId,
                savedMessage
        );
    }
}
