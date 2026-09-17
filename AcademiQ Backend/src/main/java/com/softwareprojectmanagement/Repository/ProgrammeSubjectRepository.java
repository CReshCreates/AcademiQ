package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.ProgrammeSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgrammeSubjectRepository extends JpaRepository<ProgrammeSubject, Long> {
    Optional<ProgrammeSubject> findByProgrammeSubjectId(Integer programmeSubjectId);
}
