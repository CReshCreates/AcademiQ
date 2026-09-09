package com.softwareprojectmanagement.DTO.Request;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequest {

    @NotBlank(message="Name is required!")
    private String fullName;

    @NotBlank(message="Email is required!")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid Email format!")
    private String email;

    @Size(min=5, max=15, message="Password should have at least 5 and at most 15 characters!")
    private String password;

    @NotBlank(message="Section is required!")
    private String section;

    @NotNull(message="Batch is required!")
    private Integer batch;
}
