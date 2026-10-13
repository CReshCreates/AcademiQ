
package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {

    // Count the total number of members in a project.
    @Query("""
        SELECT COUNT(pm)
        FROM ProjectMember pm
        WHERE pm.project.project_id = :projectId
        """)
    Integer countByProjectId(@Param("projectId") Integer projectId);


    // Check if a student already belongs to a project
    // associated with the given academic subject.
    @Query("""
        SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END
        FROM ProjectMember pm
        WHERE pm.user.userId = :userId
          AND pm.project.programmeSubject.subject.subjectId = :subjectId
        """)
    boolean existsByUserAndSubject(
            @Param("userId") Integer userId,
            @Param("subjectId") Integer subjectId
    );

    @Query("""
        SELECT pm
        FROM ProjectMember pm
        JOIN FETCH pm.user
        WHERE pm.project.project_id = :projectId
        ORDER BY pm.joinedAt ASC, pm.memberId ASC
        """)
    List<ProjectMember> findMembersByProjectId(
            @Param("projectId") Integer projectId
    );
}
