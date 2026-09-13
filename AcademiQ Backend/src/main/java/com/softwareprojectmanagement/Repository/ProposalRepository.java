package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Integer> {
}
