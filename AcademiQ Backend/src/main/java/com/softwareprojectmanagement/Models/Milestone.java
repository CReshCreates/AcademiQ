package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "milestones")
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "milestone_id")
    private Integer milestoneId;

    @NotNull
    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "deadline")
    private LocalDate deadline;

    @NotNull
    @Column(name = "status")
    private String status;

    @NotNull
    @Column(name = "progress")
    private Integer progress;

    @OneToMany(mappedBy = "milestone")
    private List<Task> tasks;

    @OneToMany(mappedBy = "milestone")
    private List<Submission> submission;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
}