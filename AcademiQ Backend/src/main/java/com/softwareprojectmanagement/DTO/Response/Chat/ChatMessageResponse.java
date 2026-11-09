
package com.softwareprojectmanagement.DTO.Response.Chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Integer messageId;

    private Integer projectId;

    private Integer senderId;

    private String senderName;

    private String senderRole;

    private String content;

    private LocalDateTime createdAt;
}
