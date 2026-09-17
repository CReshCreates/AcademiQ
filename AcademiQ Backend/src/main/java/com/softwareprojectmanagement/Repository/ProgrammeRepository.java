package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Registration.AvailableProgrammes;
import com.softwareprojectmanagement.Models.Programme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, Integer> {
    @Query("""
    SELECT new com.softwareprojectmanagement.DTO.Response.Registration.AvailableProgrammes(
        p.programmeId,
        p.name
    )
    FROM Programme p
""")
    List<AvailableProgrammes> getProgrammeNames();

    @Query("""
    SELECT p FROM Programme p WHERE p.programmeId = :programmeId
""")
    Programme findProgrammeById(Integer programmeId);

}
