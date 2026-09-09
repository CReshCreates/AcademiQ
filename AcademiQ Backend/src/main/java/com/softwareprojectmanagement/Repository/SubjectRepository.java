package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    @Query("""
    SELECT s
    FROM User u
    JOIN u.programme p
    JOIN p.programmeSubjects ps
    JOIN ps.subject s
    WHERE u.email = :email
      AND ps.semester = :semester
""")
    List<Subject> getSubjects(@Param("email") String email,  @Param("semester") Integer semester);
}
