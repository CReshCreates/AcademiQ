package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.Exceptions.SubjectNotFoundException;
import com.softwareprojectmanagement.Models.Batch;
import com.softwareprojectmanagement.Models.Subject;
import com.softwareprojectmanagement.Models.User;
import com.softwareprojectmanagement.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public List<Subject> getSubjects(String email){
        User user = userRepository.findByEmail(email);
        Batch batch = user.getSection().getBatch();

        LocalDate startingYear = batch.getStartYear();
        LocalDate currentYear = LocalDate.now();

        Long monthsBetween = ChronoUnit.DAYS.between(startingYear, currentYear);

        int sem = (int) (monthsBetween / 6) + 1;

        sem = Math.min(sem, 8);

        List<Subject> subjects = subjectRepository.getSubjects(email, sem);

        if(subjects.isEmpty()){
            throw new SubjectNotFoundException("There is no subject in this semester that require project submission.");
        }

        return subjects;
    }
}
