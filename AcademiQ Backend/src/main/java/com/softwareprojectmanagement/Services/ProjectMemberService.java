package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Response.ProjectMember.ProjectMemberInvitationSearchResponse;
import com.softwareprojectmanagement.Exceptions.NoSuchProjectsException;
import com.softwareprojectmanagement.Models.Project;
import com.softwareprojectmanagement.Models.User;
import com.softwareprojectmanagement.Repository.ProjectMemberRepository;
import com.softwareprojectmanagement.Repository.ProjectRepository;
import com.softwareprojectmanagement.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public List<ProjectMemberInvitationSearchResponse> searchResults(String query, Integer projectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Project project = projectRepository.findById(projectId).orElseThrow(()-> new NoSuchProjectsException("There are currently no projects with id : " + projectId));
        User user = userRepository.findByEmail(authentication.getName());
        Integer sectionId = user.getSection().getSectionId();

        Integer subjectId = project.getProgrammeSubject().getSubject().getSubjectId();

        List<User> users = userRepository.searchEligibleStudents(query, sectionId,subjectId ,projectId);

        if(users.isEmpty()){
            return List.of();
        }

        return users.stream()
                .map(u -> new ProjectMemberInvitationSearchResponse(
                        u.getUserId(),
                        u.getFullName(),
                        u.getEmail()
                ))
                .toList();
    }
}
