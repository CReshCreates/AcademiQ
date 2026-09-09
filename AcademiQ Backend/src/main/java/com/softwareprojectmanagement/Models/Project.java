package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name="projects")
@Entity
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer project_id;

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
    @Column(name="created_at")
    private LocalDateTime created_at;

    @OneToMany(mappedBy = "project")
    private List<Task> tasks;

    @OneToMany(mappedBy="project")
    private List<Proposal> proposals;

    @OneToMany(mappedBy = "project")
    private List<ProjectMember> projectMember;

    @OneToMany(mappedBy = "project")
    private List<Milestone> milestone;

    @OneToMany(mappedBy = "project")
    private List<Message> message;

    @OneToMany(mappedBy = "project")
    private List<Evaluation> evaluation;

    @ManyToOne()
    @JoinColumn(name="created_by")
    private User user;

    @ManyToOne
    @JoinColumn(name="programme_subject_id")
    private ProgrammeSubject programmeSubject;

    @OneToMany(mappedBy = "project")
    private List<ProjectActivities> projectActivity;

    @ManyToOne
    @JoinColumn(name="supervisor_id")
    private SubjectSupervisor subjectSupervisor;
}
