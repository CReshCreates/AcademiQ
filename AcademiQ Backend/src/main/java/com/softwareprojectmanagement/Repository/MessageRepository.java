
package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.Message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository
        extends JpaRepository<Message, Integer> {

    // ==========================================
    // GET PROJECT CHAT HISTORY
    // ==========================================

    @Query("""
        SELECT m
        FROM Message m
        JOIN FETCH m.user
        WHERE m.project.project_id = :projectId
        ORDER BY m.createdAt ASC, m.messageId ASC
        """)
    List<Message> findMessagesByProjectId(
            @Param("projectId") Integer projectId
    );
}
