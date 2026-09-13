package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, Integer> {
}
