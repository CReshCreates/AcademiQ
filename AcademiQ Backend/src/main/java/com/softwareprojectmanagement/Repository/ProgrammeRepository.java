package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Programme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, Integer> {
    @Query("""
        SELECT p FROM Programme p
        JOIN p.user u
        WHERE u.userId =: userId
      """)
    Programme findProgrammeForUser(@Param("userId") Integer userId);
}
