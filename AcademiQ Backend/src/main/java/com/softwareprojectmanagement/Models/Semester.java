package com.softwareprojectmanagement.Models;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="semesters")
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="semester_id")
    private Integer semesterId;

    @Column(name="name")
    private String semesterName;

    @OneToMany(mappedBy = "semester")
    private List<ProgrammeSubject> programmeSubjects;
}
