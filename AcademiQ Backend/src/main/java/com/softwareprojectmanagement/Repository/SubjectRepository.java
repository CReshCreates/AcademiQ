package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Project.AvailableSubjectsForProjectCreation;
import com.softwareprojectmanagement.Models.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    @Query("""
    SELECT new com.softwareprojectmanagement.DTO.Response.Project.AvailableSubjectsForProjectCreation(
    s.subjectId, s.name, ps.programmeSubjectId
    )
    FROM User u
    JOIN u.programme p
    JOIN p.programmeSubjects ps
    JOIN ps.subject s
    WHERE u.email = :email
      AND ps.semester.semesterId = :semester
""")
    List<AvailableSubjectsForProjectCreation> getSubjects(@Param("email") String email, @Param("semester") Integer semester);
}
