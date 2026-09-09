package com.softwareprojectmanagement.Models;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name="project_members")
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="member_id")
    private Integer memberId;

    @NotNull
    @Column(name="role")
    private String role;

    @NotNull
    @Column(name="joined_at")
    private LocalDateTime joinedAt;

    @OneToMany(mappedBy = "projectMember")
    private List<Task> task;

    @OneToMany(mappedBy = "projectMember")
    private List<Submission> submission;

    @OneToMany(mappedBy = "projectMember")
    private List<Proposal> proposal;

    @ManyToOne
    @JoinColumn(name="project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;
}
