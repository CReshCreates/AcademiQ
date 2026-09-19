package com.softwareprojectmanagement.DTO.Response.Project;

import java.time.LocalDateTime;

public interface RecentActivitiesCard {
    public String getActivityDescription();
    public LocalDateTime getActivityCreatedTime();
}
