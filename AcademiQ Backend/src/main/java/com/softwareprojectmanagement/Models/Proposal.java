package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name="proposals")
public class Proposal {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer proposalId;

    @NotNull
    @Column(name="title")
    String title;

    @NotNull
    @Column(name="description")
    String description;

    @NotNull
    @Column(name="status")
    String status;

    @Column(name="pdf_url")
    String pdfUrl;

    @Column(name="feedback")
    String feedback;

    @NotNull
    @Column(name="submitted_at")
    LocalDateTime submittedAt;

    @Column(name="reviewed_at")
    LocalDateTime reviewedAt;

    @NotNull
    @Column(name="version")
    Integer version;

    @ManyToOne
    @JoinColumn(name="project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name="member_id")
    private ProjectMember projectMember;

}
