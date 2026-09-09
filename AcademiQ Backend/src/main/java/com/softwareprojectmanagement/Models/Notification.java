package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name="notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name="notification_id")
    private Integer notificationId;

    @NotNull
    @Column(name="type")
    private String type;

    @NotNull
    @Column(name="title")
    private String title;

    @NotNull
    @Column(name="message")
    private String message;

    @NotNull
    @Column(name="is_read")
    private Boolean isRead;

    @NotNull
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;
}
