
package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Response.Chat.ChatMessageResponse;
import com.softwareprojectmanagement.Services.ProjectChatService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/messages")
public class ProjectChatController {

    private final ProjectChatService projectChatService;

    // GET PROJECT CHAT HISTORY
    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @PathVariable Integer projectId
    ) {

        List<ChatMessageResponse> messages =
                projectChatService.getChatHistory(projectId);

        return ResponseEntity.ok(messages);
    }
}
