package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Project.FeedbackCard;
import com.softwareprojectmanagement.DTO.Response.Project.RecentActivitiesCard;
import com.softwareprojectmanagement.Models.ProjectActivities;
import com.softwareprojectmanagement.Models.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectActivitiesRepository extends JpaRepository<ProjectActivities, Integer> {
    @Query("""
    SELECT pa.user.fullName AS supervisorsFullname, m.title AS milestoneName, pa.description AS feedbackDescription, pa.createdAt AS activityCreatedTime
    FROM ProjectActivities pa
    JOIN pa.project p 
    JOIN p.milestone m
    WHERE pa.project.project_id = :projectId
    AND pa.user.role = 'SUPERVISOR'
    AND (pa.activityType = 'PROPOSAL_APPROVED' 
    OR pa.activityType = 'PROPOSAL_REJECTED' 
    OR pa.activityType = 'PROPOSAL_CHANGES_REQUESTED')
    ORDER BY pa.createdAt DESC
    LIMIT 1
""")
    FeedbackCard getFeedbackCard(@Param("projectId") Integer projectId);

    @Query("""
    SELECT pa.description AS activityDescription, pa.createdAt AS activityCreatedTime
    FROM ProjectActivities pa
    WHERE pa.project.project_id = :projectId
    LIMIT 5
""")
    List<RecentActivitiesCard> getRecentActivities(@Param("projectId") Integer projectId);
}
