package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionRepository extends JpaRepository<Section, Integer> {
    String findByName(String name);


    @Query("""
    SELECT s FROM Section s WHERE s.name = :section AND s.batch.batchName = :name
""")
    Section findIdByName(String section, String name);
}
