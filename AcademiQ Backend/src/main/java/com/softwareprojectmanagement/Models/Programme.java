package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name="programmes")
public class Programme {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name="programme_id")
    private int programmeId;

    @Column(name="name")
    private String name;

    @Column(name="code")
    private String code;

    @Column(name="duration_years")
    private Integer durationYears;

    @Column(name="status")
    private String status;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "programme")
    private List<ProgrammeSubject> programmeSubjects;

    @OneToMany(mappedBy = "programme")
    private List<User> user;
}
