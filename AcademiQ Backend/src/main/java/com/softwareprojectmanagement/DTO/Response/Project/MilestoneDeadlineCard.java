package com.softwareprojectmanagement.DTO.Response.Project;

import java.time.LocalDateTime;

public interface MilestoneDeadlineCard {
    public String getMilestoneName();
    public String getMilestoneDescription();
    public LocalDateTime getMilestoneDeadline();
    public Integer getProgressPercentage();
}
