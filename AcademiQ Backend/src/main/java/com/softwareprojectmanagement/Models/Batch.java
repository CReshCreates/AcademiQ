
package com.softwareprojectmanagement.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Entity
@Table(name = "batches")
@AllArgsConstructor
@NoArgsConstructor
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Integer batchId;

    @Column(name = "name")
    private String batchName;

    @Column(name = "start_year")
    private LocalDate startYear;

    @Column(name = "end_year")
    private LocalDate endYear;

    @Column(name = "status")
    private String status;

    // ==========================================
    // WORKSPACE THAT OWNS THIS BATCH
    // ==========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "workspace_id",
            foreignKey = @ForeignKey(
                    name = "fk_batch_workspace"
            )
    )
    private Workspace workspace;

    // ==========================================
    // EXISTING SECTION RELATIONSHIP
    // ==========================================

    @OneToMany(mappedBy = "batch")
    private List<Section> section;
}
