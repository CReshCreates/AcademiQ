package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.User;
import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmail(@Email(message="Invalid email format!") String email);

    User findByEmail(String email);

    @Query("""
    SELECT u
    FROM User u
    WHERE u.role = 'STUDENT'
      AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
      
      AND u.section.sectionId = :sectionId

      AND NOT EXISTS (
          SELECT 1
          FROM ProjectMember pm
          WHERE pm.user = u
            AND pm.project.programmeSubject.subject.subjectId = :subjectId
      )

      AND NOT EXISTS (
          SELECT 1
          FROM ProjectMember currentPm
          WHERE currentPm.user = u
            AND currentPm.project.project_id = :projectId
      )
""")
    List<User> searchEligibleStudents(
            @Param("query") String query,
            @Param("sectionId") Integer sectionId,
            @Param("subjectId") Integer subjectId,
            @Param("projectId") Integer projectId
    );
}
