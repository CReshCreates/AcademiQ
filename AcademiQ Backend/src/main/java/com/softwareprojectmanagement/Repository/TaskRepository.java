
package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    // Retrieve all tasks belonging to a specific project.
    @Query("""
        SELECT t
        FROM Task t
        WHERE t.milestone.project.project_id = :projectId
        ORDER BY t.createdAt DESC
        """)
    List<Task> findTasksByProjectId(
            @Param("projectId") Integer projectId
    );


    // Retrieve all tasks under a specific milestone.
    @Query("""
        SELECT t
        FROM Task t
        WHERE t.milestone.milestoneId = :milestoneId
        ORDER BY t.createdAt DESC
        """)
    List<Task> findTasksByMilestoneId(
            @Param("milestoneId") Integer milestoneId
    );


    // Retrieve tasks assigned to a specific project member.
    @Query("""
        SELECT t
        FROM Task t
        WHERE t.projectMember.memberId = :memberId
        ORDER BY t.deadline ASC
        """)
    List<Task> findTasksByMemberId(
            @Param("memberId") Integer memberId
    );

}
