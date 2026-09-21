package com.softwareprojectmanagement.Services;

import com.softwareprojectmanagement.DTO.Request.Invitation.InvitationActionRequest;
import com.softwareprojectmanagement.DTO.Request.Invitation.InvitationRequest;
import com.softwareprojectmanagement.DTO.Response.Invitation.MyInvitationsResponse;
import com.softwareprojectmanagement.DTO.Response.Invitation.NotificationDetailsForInvitation;
import com.softwareprojectmanagement.Exceptions.NoInvitationFoundException;
import com.softwareprojectmanagement.Exceptions.NoSuchProjectsException;
import com.softwareprojectmanagement.Exceptions.ProjectMembersCountExceededException;
import com.softwareprojectmanagement.Models.*;
import com.softwareprojectmanagement.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationService {
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationRepository notificationRepository;
    private final ProjectActivitiesRepository projectActivitiesRepository;

    @Transactional
    public Integer inviteToProject(InvitationRequest invitationRequest) {

        if(projectMemberRepository.countByProjectId(invitationRequest.getProjectId())>4){
            throw new ProjectMembersCountExceededException("You can have only 3 project members!!!");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User invitedBy = userRepository.findByEmail(authentication.getName());
        if(invitedBy == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Project project = projectRepository.findById(invitationRequest.getProjectId()).
                orElseThrow(()-> new NoSuchProjectsException("Project not found"));

        User invitedUser = userRepository.findById(invitationRequest.getInvitedUserId()).orElseThrow(() -> new UsernameNotFoundException("User to be invited not found"));

        Invitation invitation = new Invitation();
        invitation.setProject(project);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitedBy(invitedBy);
        invitation.setInvitedUser(invitedUser);
        invitation.setCreatedAt(LocalDateTime.now());
        Integer savedInvitationId= Math.toIntExact(invitationRepository.save(invitation).getInvitationId());

        Notification notification = new Notification();
        notification.setTitle("Project Invitation");
        notification.setMessage(
                "You have been invited by "
                + invitedBy.getFullName()
                + " to participate in project "
                + project.getTitle()
                + " for your "
                + project.getProgrammeSubject().getSubject().getName()
                + " project."
        );
        notification.setCreatedAt(LocalDateTime.now());
        notification.setType("INVITATION");
        notification.setIsRead(false);
        notification.setUser(invitedUser);
        notification.setInvitationId(savedInvitationId);
        notificationRepository.save(notification);

        return invitedBy.getUserId();
    }

    public List<MyInvitationsResponse> getMyProjectsInvitation(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String invitedTo = userRepository.findByEmail(authentication.getName()).getEmail();

        List<Integer> invitationIds = invitationRepository.getInvitationIdsByEmail(invitedTo);
        List<NotificationDetailsForInvitation> notificationDetailsForInvitations = notificationRepository.getNotificationDetails(invitedTo);

        Map<Integer,NotificationDetailsForInvitation> notificationDetailsForInvitationMap =
                notificationDetailsForInvitations.stream()
                        .collect(Collectors.toMap(
                                NotificationDetailsForInvitation::getInvitationId,
                                notificationDetailsForInvitation -> notificationDetailsForInvitation
                        ));

        List<MyInvitationsResponse> response = invitationIds.stream()
                .map(invitationId -> new MyInvitationsResponse(
                        invitationId,
                        notificationDetailsForInvitationMap.get(invitationId)
                ))
                .toList();

        return response;
    }

    @Transactional
    public String invitationAction(InvitationActionRequest invitationActionRequest) throws NoSuchMethodException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(authentication.getName());

        if(projectMemberRepository.countByProjectId(invitationActionRequest.getProjectId())>4){
            throw new ProjectMembersCountExceededException("There are already 3 project members in this project!");
        }
        if("ACCEPT".equalsIgnoreCase(invitationActionRequest.getAction())){
            Invitation invitation = invitationRepository.findById(invitationActionRequest.getInvitationId())
                    .orElseThrow(()-> new NoInvitationFoundException("There are no invitations with this id:" + invitationActionRequest.getInvitationId()));
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitation.setRespondedAt(LocalDateTime.now());
            Invitation savedInvite = invitationRepository.save(invitation);

            ProjectActivities projectActivities = new ProjectActivities();
            projectActivities.setActivityType("MEMBER_ADDED");
            projectActivities.setDescription(user.getFullName() + " has accepted the invitation. " + user.getFullName() + " is a member of this project.");
            projectActivities.setCreatedAt(LocalDateTime.now());
            projectActivities.setUser(savedInvite.getInvitedBy());
            projectActivities.setProject(savedInvite.getProject());
            projectActivitiesRepository.save(projectActivities);

            return "ACCEPTED";
        }
        else if("DECLINE".equalsIgnoreCase(invitationActionRequest.getAction())){
            Invitation invitation = invitationRepository.findById(invitationActionRequest.getInvitationId())
                    .orElseThrow(()-> new NoInvitationFoundException("There are no invitations with this id:" + invitationActionRequest.getInvitationId()));
            invitation.setStatus(InvitationStatus.REJECTED);
            invitation.setRespondedAt(LocalDateTime.now());
            Invitation savedInvite = invitationRepository.save(invitation);

            ProjectActivities projectActivities = new ProjectActivities();
            projectActivities.setActivityType("PROJECT_UPDATED");
            projectActivities.setDescription(user.getFullName() + " has rejected the team invitation.");
            projectActivities.setCreatedAt(LocalDateTime.now());
            projectActivities.setUser(savedInvite.getInvitedBy());
            projectActivities.setProject(savedInvite.getProject());
            projectActivitiesRepository.save(projectActivities);

            return "REJECTED";
        }
        else{
            throw new NoSuchMethodException("There are no such actions!!!");
        }

    }
}
