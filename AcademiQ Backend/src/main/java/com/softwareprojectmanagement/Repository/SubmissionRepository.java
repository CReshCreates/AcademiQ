package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
}
