package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name="tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer taskId;

    @NotNull
    @Column(name="title")
    private String title;

    @NotNull
    @Column(name="description")
    private String description;

    @NotNull
    @Column(name="status")
    private String status;

    @NotNull
    @Column(name="priority")
    private String priority;

    @NotNull
    @Column(name="deadline")
    private LocalDate deadline;

    @NotNull
    @Column(name="progress")
    private Integer progress;

    @NotNull
    @Column(name="created_at")
    private LocalDate createdAt;


    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name="member_id")
    private ProjectMember projectMember;
}
