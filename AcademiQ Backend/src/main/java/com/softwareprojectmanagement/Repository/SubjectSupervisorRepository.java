package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Project.AvailableSupervisor;
import com.softwareprojectmanagement.Models.SubjectSupervisor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectSupervisorRepository extends JpaRepository<SubjectSupervisor,Integer> {

    @Query("""
    SELECT new com.softwareprojectmanagement.DTO.Response.Project.AvailableSupervisor(s.subjectSupervisorId, s.user.fullName, s.user.userId)
    FROM SubjectSupervisor s 
    WHERE s.programmeSubject.programmeSubjectId = :programmeSubjectId
""")
    List<AvailableSupervisor> getSupervisorBySubject(Integer programmeSubjectId);

    Optional<SubjectSupervisor>
    findByUser_UserIdAndProgrammeSubject_ProgrammeSubjectId(
            Integer supervisorId,
            Integer programmeSubjectId
    );
}
