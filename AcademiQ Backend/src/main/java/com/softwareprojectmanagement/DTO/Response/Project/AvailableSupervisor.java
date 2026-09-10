package com.softwareprojectmanagement.DTO.Response.Project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailableSupervisor {

    private Integer supervisorId;
    private String supervisorName;

}
