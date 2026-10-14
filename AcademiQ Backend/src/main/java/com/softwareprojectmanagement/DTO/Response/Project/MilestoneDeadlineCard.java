
package com.softwareprojectmanagement.DTO.Response.Project;

import java.time.LocalDate;

public interface MilestoneDeadlineCard {

    String getMilestoneName();

    String getMilestoneDescription();

    LocalDate getMilestoneDeadline();

    Integer getProgressPercentage();
}
