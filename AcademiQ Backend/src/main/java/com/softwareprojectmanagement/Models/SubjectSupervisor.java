package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name="subject_supervisors")
public class SubjectSupervisor {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name="subject_supervisor_id")
    private Integer subjectSupervisorId;

    @ManyToOne
    @JoinColumn(name="programme_subject_id")
    private ProgrammeSubject programmeSubject;

    @ManyToOne
    @JoinColumn(name="supervisor_id")
    private User user;
}
