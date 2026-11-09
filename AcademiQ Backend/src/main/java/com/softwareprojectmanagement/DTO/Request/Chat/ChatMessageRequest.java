
package com.softwareprojectmanagement.DTO.Request.Chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    @NotBlank(message = "Message cannot be empty.")
    @Size(
            max = 2000,
            message = "Message cannot exceed 2000 characters."
    )
    private String content;
}
