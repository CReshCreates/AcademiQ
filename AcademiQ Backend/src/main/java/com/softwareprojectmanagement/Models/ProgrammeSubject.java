package com.softwareprojectmanagement.Models;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name="programme_subjects")
public class ProgrammeSubject {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name="programme_subject_id")
    private Integer programmeSubjectId;

    @ManyToOne
    @JoinColumn(name="programme_id")
    private Programme programme;

    @ManyToOne
    @JoinColumn(name="subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name="semester_id")
    private Semester semester;

    @OneToMany(mappedBy = "programmeSubject")
    private List<SubjectSupervisor> subjectSupervisors;

    @OneToMany(mappedBy = "programmeSubject")
    private List<Project> projects;
}
