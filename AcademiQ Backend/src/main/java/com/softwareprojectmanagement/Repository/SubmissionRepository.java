package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionQueueResponse;
import com.softwareprojectmanagement.Models.Submission;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer> {

    // GET PENDING SUBMISSIONS FOR SUPERVISOR
    @Query("""
        SELECT new com.softwareprojectmanagement.DTO.Response.Submission.SubmissionQueueResponse(
            s.submissionId,
            p.title,
            s.submittedAt,
            m.title,
            pm.memberId,
            pm.user.fullName
        )
        FROM Submission s
        JOIN s.milestone m
        JOIN m.project p
        JOIN s.projectMember pm
        WHERE p.supervisor.userId = :userId
          AND s.status = 'PENDING'
        ORDER BY s.submittedAt ASC, s.submissionId ASC
        """)
    List<SubmissionQueueResponse> findPendingProjectReviews(
            @Param("userId") Integer userId
    );


    @Query("""
    SELECT s FROM Submission s WHERE s.submissionId = :submissionId
""")
    Submission getPdfNameBySubmissionId(Integer submissionId);

    // GET LATEST SUBMISSION FOR A MILESTONE
    @Query("""
        SELECT s
        FROM Submission s
        WHERE s.milestone.milestoneId = :milestoneId
        ORDER BY s.submissionVersion DESC,
                 s.submissionId DESC
        LIMIT 1
        """)
    Submission findLatestSubmissionByMilestoneId(
            @Param("milestoneId") Integer milestoneId
    );

    // GET SUBMISSION HISTORY FOR A PROJECT
    @Query("""
        SELECT s
        FROM Submission s
        JOIN FETCH s.milestone m
        WHERE m.project.project_id = :projectId
        ORDER BY s.submittedAt DESC,
                 s.submissionVersion DESC,
                 s.submissionId DESC
        """)
    List<Submission> findSubmissionHistoryByProjectId(
            @Param("projectId") Integer projectId
    );


    // ==========================================
    // LOCK SUBMISSION DURING SUPERVISOR REVIEW
    // ==========================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Submission s
        WHERE s.submissionId = :submissionId
        """)
    Optional<Submission> findByIdForUpdate(
            @Param("submissionId") Integer submissionId
    );


}
