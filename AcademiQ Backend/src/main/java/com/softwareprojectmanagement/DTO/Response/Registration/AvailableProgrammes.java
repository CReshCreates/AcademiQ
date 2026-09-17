package com.softwareprojectmanagement.DTO.Response.Registration;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailableProgrammes {
    Integer programmeId;
    String programme;
}