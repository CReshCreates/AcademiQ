package com.softwareprojectmanagement.DTO.Response.Project;

import java.time.LocalDateTime;

public interface FeedbackCard {
    public String getSupervisorsFullname();
    public String getMilestoneName();
    public String getFeedbackDescription();
    public LocalDateTime getActivityCreatedTime();
}
