package com.softwareprojectmanagement.DTO.Response.Project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyProjectsPageInfo {
    ProjectKPICards projectKPICards;
    List<TeamMembers> teamMembers;
    MilestoneDeadlineCard milestoneDeadlineCard;
    FeedbackCard feedbackCard;
    List<RecentActivitiesCard> recentActivitiesCard;
}
