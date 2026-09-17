package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.ProjectActivities;
import com.softwareprojectmanagement.Models.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectActivitiesRepository extends JpaRepository<ProjectActivities, Integer> {
}
