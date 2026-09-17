package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name="submissions")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer submissionId;

    @NotNull
    @Column(name="file_url")
    private String fileUrl;

    @NotNull
    @Column(name="submitted_at")
    private LocalDateTime submittedAt;

    @NotNull
    @Column(name="status")
    private String status;

    @NotNull
    @Column(name="file_name")
    private String fileName;

    @NotNull
    @Column(name="submission_version")
    private Integer submissionVersion;

    @ManyToOne
    @JoinColumn(name="milestone_id")
    private Milestone milestone;

    @ManyToOne
    @JoinColumn(name="member_id")
    private ProjectMember projectMember;

    @OneToMany(mappedBy = "submission")
    private List<Comment> comment;
}
