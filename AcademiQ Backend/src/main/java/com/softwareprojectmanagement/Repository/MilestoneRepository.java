package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Project.MilestoneDeadlineCard;
import com.softwareprojectmanagement.Models.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Integer> {
    @Query("""
    SELECT m.title AS milestoneName, m.description AS milestoneDescription, m.deadline AS milestoneDeadline, m.progress AS progressPercentage
    FROM Milestone m 
    WHERE m.project.project_id = :projectId
    LIMIT 1
""")
    MilestoneDeadlineCard getMilestoneDeadline(@Param("projectId") Integer projectId);
}
