
package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Subtask;
import com.softwareprojectmanagement.Models.SubtaskStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubtaskRepository
        extends JpaRepository<Subtask, Long> {

    // GET ALL SUBTASKS OF A TASK

    @Query("""
        SELECT s
        FROM Subtask s
        WHERE s.task.taskId = :taskId
        ORDER BY s.subtaskId ASC
        """)
    List<Subtask> findByTaskId(
            @Param("taskId") Integer taskId
    );


    // COUNT ALL SUBTASKS OF A TASK

    @Query("""
        SELECT COUNT(s)
        FROM Subtask s
        WHERE s.task.taskId = :taskId
        """)
    long countByTaskId(
            @Param("taskId") Integer taskId
    );

    // COUNT SUBTASKS BY STATUS

    @Query("""
        SELECT COUNT(s)
        FROM Subtask s
        WHERE s.task.taskId = :taskId
          AND s.status = :status
        """)
    long countByTaskIdAndStatus(
            @Param("taskId") Integer taskId,
            @Param("status") SubtaskStatus status
    );

    // CHECK SUBTASK OWNERSHIP

    @Query("""
        SELECT CASE WHEN COUNT(s) > 0
                    THEN true ELSE false END
        FROM Subtask s
        WHERE s.subtaskId = :subtaskId
          AND s.task.taskId = :taskId
        """)
    boolean existsByIdAndTaskId(
            @Param("subtaskId") Long subtaskId,
            @Param("taskId") Integer taskId
    );
}
