package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    private Integer userId;

    @Column(name="full_name")
    private String fullName;

    @Email
    @Column(name="email")
    private String email;

    @Column(name="password_hash")
    private String passwordHash;

    @Column(name="role")
    private String role;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user")
    private List<ProjectMember> projectMember;

    @OneToMany(mappedBy = "user")
    private List<Project> project;

    @OneToMany(mappedBy = "user")
    private List<Notification> notification;

    @OneToMany(mappedBy = "user")
    private List<Message> message;

    @OneToMany(mappedBy = "user")
    private List<Evaluation> evaluation;

    @OneToMany(mappedBy = "user")
    private List<Comment> comment;

    @ManyToOne
    @JoinColumn(name="section_id")
    private Section section;

    @OneToMany(mappedBy = "user")
    private List<ProjectActivities> projectActivities;

    @OneToMany(mappedBy = "user")
    private List<SubjectSupervisor> subjectSupervisor;

    @ManyToOne
    @JoinColumn(name="programme_id")
    private Programme programme;
}
