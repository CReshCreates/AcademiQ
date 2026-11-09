
package com.softwareprojectmanagement.DTO.Response.Project;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class MyProjectsPageInfo {

    // BASIC PROJECT INFORMATION
    private Integer projectId;
    private String projectTitle;
    private String projectDescription;
    private String projectStatus;
    private LocalDateTime createdAt;
    private Integer overallProgress;

    // ASSIGNED SUPERVISOR
    private Integer supervisorId;
    private String supervisorName;

    // EXISTING PROJECT OVERVIEW DATA
    private ProjectKPICards projectKPICards;
    private List<TeamMembers> teamMembers;
    private MilestoneDeadlineCard milestoneDeadlineCard;
    private FeedbackCard feedbackCard;
    private List<RecentActivitiesCard> recentActivitiesCard;

    public MyProjectsPageInfo(
            ProjectKPICards projectKPICards,
            List<TeamMembers> teamMembers,
            MilestoneDeadlineCard milestoneDeadlineCard,
            FeedbackCard feedbackCard,
            List<RecentActivitiesCard> recentActivitiesCard
    ) {
        this.projectKPICards = projectKPICards;
        this.teamMembers = teamMembers;
        this.milestoneDeadlineCard = milestoneDeadlineCard;
        this.feedbackCard = feedbackCard;
        this.recentActivitiesCard = recentActivitiesCard;
    }
}
