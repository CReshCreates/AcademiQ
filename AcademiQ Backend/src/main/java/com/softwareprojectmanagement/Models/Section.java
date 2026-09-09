package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@Table(name = "sections")
@AllArgsConstructor
@NoArgsConstructor
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="section_id")
    private Integer sectionId;

    @NotBlank
    @Column(name="name")
    private String name;

    @ManyToOne
    @JoinColumn(name="batch_id")
    private Batch batch;

    @OneToMany(mappedBy = "section")
    private List<User> user;
}
