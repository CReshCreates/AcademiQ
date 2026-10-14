
package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Task;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    // GET TASKS BELONGING TO A PROJECT
    @Query("""
        SELECT t
        FROM Task t
        WHERE t.milestone.project.project_id = :projectId
        ORDER BY t.createdAt DESC
        """)
    List<Task> findTasksByProjectId(
            @Param("projectId") Integer projectId
    );


    // GET TASKS UNDER A MILESTONE
    @Query("""
        SELECT t
        FROM Task t
        WHERE t.milestone.milestoneId = :milestoneId
        ORDER BY t.createdAt DESC
        """)
    List<Task> findTasksByMilestoneId(
            @Param("milestoneId") Integer milestoneId
    );

    // GET TASKS ASSIGNED TO A MEMBER
    @Query("""
        SELECT t
        FROM Task t
        WHERE t.projectMember.memberId = :memberId
        ORDER BY t.deadline ASC
        """)
    List<Task> findTasksByMemberId(
            @Param("memberId") Integer memberId
    );

    // LOCK TASK FOR SUBTASK MODIFICATIONS
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT t
        FROM Task t
        WHERE t.taskId = :taskId
        """)
    Optional<Task> findByIdForUpdate(
            @Param("taskId") Integer taskId
    );

    @Query("""
        SELECT t
        FROM Task t
        JOIN t.projectMember pm
        JOIN pm.user u
        WHERE u.userId = :userId
        ORDER BY t.deadline ASC, t.taskId ASC
        """)
    List<Task> findAllTasksAssignedToUser(
            @Param("userId") Integer userId
    );


    // GET AVERAGE TASK PROGRESS FOR A PROJECT
    @Query("""
        SELECT AVG(t.progress)
        FROM Task t
        WHERE t.milestone.project.project_id = :projectId
        """)
    Double getAverageTaskProgressByProjectId(
            @Param("projectId") Integer projectId
    );

}
