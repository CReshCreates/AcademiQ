
package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Project.MilestoneDeadlineCard;
import com.softwareprojectmanagement.Models.Milestone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneRepository
        extends JpaRepository<Milestone, Integer> {

    //PROJECT OVERVIEW MILESTONE CARD
    @Query("""
        SELECT
            m.title AS milestoneName,
            m.description AS milestoneDescription,
            m.deadline AS milestoneDeadline,
            m.progress AS progressPercentage
        FROM Milestone m
        WHERE m.project.project_id = :projectId
        LIMIT 1
        """)
    MilestoneDeadlineCard getMilestoneDeadline(
            @Param("projectId") Integer projectId
    );

    // GET ALL MILESTONES OF A PROJECT
    @Query("""
        SELECT m
        FROM Milestone m
        WHERE m.project.project_id = :projectId
        ORDER BY m.milestoneId ASC
        """)
    List<Milestone> findMilestonesByProjectId(
            @Param("projectId") Integer projectId
    );


}
