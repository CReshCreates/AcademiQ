package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name="evaluations")
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="evaluation_id")
    private Integer evaluationId;

    @NotNull
    @Column(name="score")
    private Float score;

    @NotNull
    @Column(name="feedback")
    private String feedback;

    @NotNull
    @Column(name="evaluated_at")
    private LocalDateTime evaluatedAt;

    @ManyToOne
    @JoinColumn(name="project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name="evaluator_id")
    private User user;
}
