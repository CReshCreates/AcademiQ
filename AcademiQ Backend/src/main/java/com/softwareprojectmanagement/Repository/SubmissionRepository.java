package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Submission.SubmissionQueueResponse;
import com.softwareprojectmanagement.Models.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
    @Query("""
     SELECT new com.softwareprojectmanagement.DTO.Response.Submission.SubmissionQueueResponse
     (s.submissionId, p.title, s.submittedAt, m.title, pm.memberId, pm.user.fullName)
     FROM Submission s 
     JOIN s.milestone m 
     JOIN m.project p 
     JOIN p.projectMember pm
     WHERE p.supervisor.userId = :userId
     AND s.status = 'PENDING'
     ORDER BY s.submittedAt DESC
""")
    List<SubmissionQueueResponse> findPendingProjectReviews(int userId);

    @Query("""
    SELECT s FROM Submission s WHERE s.submissionId = :submissionId
""")
    Submission getPdfNameBySubmissionId(Integer submissionId);
}
