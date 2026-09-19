package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Project.CreatedProjectResponse;
import com.softwareprojectmanagement.DTO.Response.Project.ProjectKPICards;
import com.softwareprojectmanagement.DTO.Response.Project.TeamMembers;
import com.softwareprojectmanagement.Models.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    @Query("""
    SELECT new com.softwareprojectmanagement.DTO.Response.Project.CreatedProjectResponse(
        p.project_id,
        p.title,
        p.supervisor.fullName,
        p.programmeSubject.semester.semesterName,
        p.programmeSubject.subject.name,
        0,
        'Proposal',
        p.created_at
    )
    FROM Project p
    WHERE p.project_id = :projectId
""")
    CreatedProjectResponse getCreatedProject(
            @Param("projectId") Integer projectId
    );

    @Query("""
    SELECT new com.softwareprojectmanagement.DTO.Response.Project.CreatedProjectResponse(
        p.project_id,
        p.title,
        p.supervisor.fullName,
        p.programmeSubject.semester.semesterName,
        p.programmeSubject.subject.name,
        CAST(COALESCE(ROUND(AVG(t.progress)), 0) AS INTEGER ),
        p.status,
        p.created_at
    )
    FROM Project p
    JOIN p.projectMember pm
    LEFT JOIN p.milestone m
    LEFT JOIN m.tasks t
    WHERE pm.user.userId = :userId
    
    GROUP BY
        p.project_id,
        p.title,
        p.supervisor.fullName,
        p.programmeSubject.semester.semesterName,
        p.programmeSubject.subject.name,
        p.status
""")
    List<CreatedProjectResponse> getMyProjects(@Param("userId") Integer userId);

    @Query("""
    SELECT
        (SELECT COUNT(t)
         FROM Task t
         WHERE t.milestone.project.project_id = :projectId) AS totalTasks,

        (SELECT COUNT(t)
         FROM Task t
         WHERE t.milestone.project.project_id = :projectId
         AND t.status = 'COMPLETED') AS completedTasks,

        (SELECT COUNT(m)
         FROM Milestone m
         WHERE m.project.project_id = :projectId) AS totalMilestones,

        (SELECT COUNT(m)
         FROM Milestone m
         WHERE m.project.project_id = :projectId
         AND m.status = 'COMPLETED') AS completedMilestones
""")
    ProjectKPICards getProjectKPICards(@Param("projectId") Integer projectId);

    @Query("""
    SELECT pm.user.fullName AS fullName, pm.role AS role, COUNT(t) AS taskedAssignedNumber
    FROM ProjectMember pm
    JOIN pm.task t
    WHERE pm.project.project_id = :projectId
    GROUP BY pm.user.fullName, pm.role
""")
    List<TeamMembers> getTeamMembers(@Param("projectId") Integer projectId);
}
