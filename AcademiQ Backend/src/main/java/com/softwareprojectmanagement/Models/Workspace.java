
package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workspace_id")
    private Integer workspaceId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(
            name = "slug",
            nullable = false,
            unique = true,
            length = 160
    )
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "workspace_type",
            nullable = false,
            length = 30
    )
    private WorkspaceType workspaceType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private WorkspaceStatus status = WorkspaceStatus.ACTIVE;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime updatedAt;

    public enum WorkspaceType {
        ACADEMIC,
        ORGANIZATIONAL
    }

    public enum WorkspaceStatus {
        ACTIVE,
        SUSPENDED,
        ARCHIVED
    }
}
