package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Registration.AvailableBatches;
import com.softwareprojectmanagement.Models.Batch;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Integer> {
    @Query("""
        SELECT new com.softwareprojectmanagement.DTO.Response.Registration.AvailableBatches(
            b.batchName
        )
        FROM Batch b WHERE b.status =:status
    """)
    List<AvailableBatches> findAllByStatus(@Param("status") String status);

    @Query("""
        SELECT b.batchId FROM Batch b WHERE b.batchName = :name
""")
    Integer getIdByName(String name);
}
