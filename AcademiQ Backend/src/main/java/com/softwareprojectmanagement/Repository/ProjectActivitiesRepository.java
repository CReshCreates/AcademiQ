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

    // GET LATEST SUPERVISOR FEEDBACK
    @Query("""
        SELECT
            c.user.fullName AS supervisorsFullname,
            m.title AS milestoneName,
            c.content AS feedbackDescription,
            c.createdAt AS activityCreatedTime
        FROM Comment c
        JOIN c.submission s
        JOIN s.milestone m
        JOIN m.project p
        WHERE p.project_id = :projectId
          AND p.supervisor IS NOT NULL
          AND c.user.userId = p.supervisor.userId
        ORDER BY c.createdAt DESC, c.commentId DESC
        LIMIT 1
        """)
    FeedbackCard getFeedbackCard(
            @Param("projectId") Integer projectId
    );



    // GET FIVE MOST RECENT PROJECT ACTIVITIES
    @Query("""
        SELECT
            pa.description AS activityDescription,
            pa.createdAt AS activityCreatedTime
        FROM ProjectActivities pa
        WHERE pa.project.project_id = :projectId
        ORDER BY pa.createdAt DESC, pa.activityId DESC
        LIMIT 5
        """)
    List<RecentActivitiesCard> getRecentActivities(
            @Param("projectId") Integer projectId
    );

}
