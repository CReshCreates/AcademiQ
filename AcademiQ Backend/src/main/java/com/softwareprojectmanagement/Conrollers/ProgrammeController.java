package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Response.Registration.AvailableProgrammes;
import com.softwareprojectmanagement.Models.Programme;
import com.softwareprojectmanagement.Services.ProgrammeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/programme")
public class ProgrammeController {

    private final ProgrammeService programmeService;

    @GetMapping("/getAllProgrammes")
    public ResponseEntity<List<AvailableProgrammes>> getAllProgrammes(){
        return ResponseEntity.ok(programmeService.getAllProgrammes());
    }

}
