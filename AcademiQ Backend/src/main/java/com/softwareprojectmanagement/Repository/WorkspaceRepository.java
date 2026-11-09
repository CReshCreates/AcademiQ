
package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkspaceRepository
        extends JpaRepository<Workspace, Integer> {

    // Find a workspace by its public URL slug.
    Optional<Workspace> findBySlug(String slug);

    // Check whether a workspace slug is already taken.
    boolean existsBySlug(String slug);

}
