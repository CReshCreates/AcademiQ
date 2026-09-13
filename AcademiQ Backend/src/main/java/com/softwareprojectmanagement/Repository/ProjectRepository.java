package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.DTO.Response.Project.CreatedProjectResponse;
import com.softwareprojectmanagement.Models.Programme;
import com.softwareprojectmanagement.Models.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


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
        'Proposal'
    )
    FROM Project p
    WHERE p.project_id = :projectId
""")
    CreatedProjectResponse getCreatedProject(
            @Param("projectId") Integer projectId
    );
}
