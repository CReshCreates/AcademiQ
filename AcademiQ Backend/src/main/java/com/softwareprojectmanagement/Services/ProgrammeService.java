package com.softwareprojectmanagement.Services;


import com.softwareprojectmanagement.DTO.Response.Registration.AvailableProgrammes;
import com.softwareprojectmanagement.Exceptions.NoProgrammesRegisteredException;
import com.softwareprojectmanagement.Repository.ProgrammeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgrammeService {
    private final ProgrammeRepository programmeRepository;

    public List<AvailableProgrammes> getAllProgrammes(){
        if(programmeRepository.findAll().size()==0){
            throw new NoProgrammesRegisteredException("No Programmes are currently registered. Please login as admin to register programmes.");
        }
        return programmeRepository.getProgrammeNames();
    }
}
