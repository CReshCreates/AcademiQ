package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {

    @Query("""
    SELECT COUNT(pm) FROM ProjectMember pm WHERE pm.project.project_id = :projectId
""")
    Integer countByProjectId(Integer projectId);
}
