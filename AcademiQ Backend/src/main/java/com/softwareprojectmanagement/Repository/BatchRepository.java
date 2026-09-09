package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Batch;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Integer> {
    List<Batch> findAllByStatus(String active);

    @Query("""
        SELECT b.batchId FROM Batch b WHERE b.batchName = :name
""")
    Integer getIdByName(String name);
}
