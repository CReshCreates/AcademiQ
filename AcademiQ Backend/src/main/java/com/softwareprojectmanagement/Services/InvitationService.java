
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

import org.springframework.security.access.AccessDeniedException;
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


    // SEND PROJECT INVITATION

    @Transactional
    public Integer inviteToProject(InvitationRequest invitationRequest) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User invitedBy = userRepository.findByEmail(
                authentication.getName()
        );

        if (invitedBy == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Project project = projectRepository
                .findByIdForUpdate(invitationRequest.getProjectId())
                .orElseThrow(() ->
                        new NoSuchProjectsException(
                                "Project not found."
                        )
                );


        // Only students can send project invitations.
        if (!"STUDENT".equals(invitedBy.getRole())) {
            throw new AccessDeniedException(
                    "Only students can invite project members."
            );
        }

        // The project creator is the designated group leader.
        if (project.getUser() == null
                || !project.getUser().getUserId()
                .equals(invitedBy.getUserId())) {

            throw new AccessDeniedException(
                    "Only the project leader can invite students."
            );
        }

        User invitedUser = userRepository
                .findById(invitationRequest.getInvitedUserId())
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User to be invited not found"
                        )
                );


        // Only students can become project group members.
        if (!"STUDENT".equals(invitedUser.getRole())) {
            throw new IllegalStateException(
                    "Only students can be invited to a project."
            );
        }

        // Verify the student's academic profile.
        if (invitedUser.getProgramme() == null
                || invitedUser.getSection() == null
                || invitedUser.getSection().getBatch() == null) {

            throw new IllegalStateException(
                    "The invited student's academic profile is incomplete."
            );
        }

        // Get the programme associated with the project subject.
        Integer projectProgrammeId = project
                .getProgrammeSubject()
                .getProgramme()
                .getProgrammeId();

        // Get the invited student's programme.
        Integer studentProgrammeId = invitedUser
                .getProgramme()
                .getProgrammeId();

        // Both must belong to the same academic programme.
        if (!projectProgrammeId.equals(studentProgrammeId)) {
            throw new IllegalStateException(
                    "The invited student does not belong to "
                            + "the academic programme of this project."
            );
        }


        // VALIDATE SAME BATCH AND SECTION

        // Ensure the project leader has a complete academic profile.
        if (invitedBy.getSection() == null
                || invitedBy.getSection().getBatch() == null) {

            throw new IllegalStateException(
                    "Your academic section or batch information is incomplete."
            );
        }

        // Get the batch IDs of both students.
        Integer leaderBatchId = invitedBy
                .getSection()
                .getBatch()
                .getBatchId();

        Integer invitedStudentBatchId = invitedUser
                .getSection()
                .getBatch()
                .getBatchId();

        // Students must belong to the same batch.
        if (!leaderBatchId.equals(invitedStudentBatchId)) {

            throw new IllegalStateException(
                    "You can only invite students from your own academic batch."
            );
        }

        // Get the section IDs of both students.
        Integer leaderSectionId = invitedBy
                .getSection()
                .getSectionId();

        Integer invitedStudentSectionId = invitedUser
                .getSection()
                .getSectionId();

        // Students must belong to the same section.
        if (!leaderSectionId.equals(invitedStudentSectionId)) {

            throw new IllegalStateException(
                    "You can only invite students from your own section."
            );
        }



        Invitation invitation = new Invitation();


        boolean alreadyInvited =
                invitationRepository.existsInvitationByProjectAndUserAndStatus(
                        project.getProject_id(),
                        invitedUser.getUserId(),
                        InvitationStatus.PENDING
                );

        if (alreadyInvited) {
            throw new IllegalStateException(
                    "This student already has a pending invitation for this project."
            );
        }

        // CHECK EXISTING PROJECT PARTICIPATION

        // Retrieve the academic subject of this project.
        Integer subjectId = project
                .getProgrammeSubject()
                .getSubject()
                .getSubjectId();

        // Check whether the invited student already
        // participates in any project for this subject.
        boolean alreadyParticipating =
                projectMemberRepository.existsByUserAndSubject(
                        invitedUser.getUserId(),
                        subjectId
                );

        // Prevent inviting an existing project participant.
        if (alreadyParticipating) {
            throw new IllegalStateException(
                    "This student already belongs to a project "
                            + "for this academic subject."
            );
        }
        
        // VALIDATE AVAILABLE PROJECT POSITIONS

        // Count students who are already project members.
        int currentMembers =
                projectMemberRepository.countByProjectId(
                        project.getProject_id()
                );

        // Count invitations that are awaiting responses.
        long pendingInvitations =
                invitationRepository.countInvitationsByProjectAndStatus(
                        project.getProject_id(),
                        InvitationStatus.PENDING
                );

        // Existing members and pending invitations both
        // occupy a position under the reservation policy.
        long reservedPositions =
                currentMembers + pendingInvitations;

        // A project can reserve a maximum of three positions.
        if (reservedPositions >= 3) {
            throw new ProjectMembersCountExceededException(
                    "This project has no available positions. "
                            + "Existing members and pending invitations "
                            + "have reached the maximum of 3 students."
            );
        }


        invitation.setProject(project);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitedBy(invitedBy);
        invitation.setInvitedUser(invitedUser);
        invitation.setCreatedAt(LocalDateTime.now());

        Integer savedInvitationId = Math.toIntExact(
                invitationRepository.save(invitation).getInvitationId()
        );

        // Create notification for invited student.
        Notification notification = new Notification();

        notification.setTitle("Project Invitation");

        notification.setMessage(
                "You have been invited by "
                        + invitedBy.getFullName()
                        + " to participate in project "
                        + project.getTitle()
                        + " for your "
                        + project.getProgrammeSubject()
                        .getSubject().getName()
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


    // GET MY PROJECT INVITATIONS

    public List<MyInvitationsResponse> getMyProjectsInvitation() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByEmail(
                authentication.getName()
        );

        if (user == null) {
            throw new UsernameNotFoundException(
                    "Authenticated user not found."
            );
        }

        String invitedTo = user.getEmail();

        List<Integer> invitationIds =
                invitationRepository.getInvitationIdsByEmail(invitedTo);

        List<NotificationDetailsForInvitation> notificationDetails =
                notificationRepository.getNotificationDetails(invitedTo);

        Map<Integer, NotificationDetailsForInvitation> notificationMap =
                notificationDetails.stream()
                        .collect(Collectors.toMap(
                                NotificationDetailsForInvitation::getInvitationId,
                                detail -> detail
                        ));

        return invitationIds.stream()
                .map(invitationId -> new MyInvitationsResponse(
                        invitationId,
                        notificationMap.get(invitationId)
                ))
                .toList();
    }


    // ACCEPT OR DECLINE INVITATION


    @Transactional
    public String invitationAction(
            InvitationActionRequest invitationActionRequest
    ) throws NoSuchMethodException {

        // STEP 1: Identify and lock the authenticated student.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Authentication is required."
            );
        }

        User user = userRepository
                .findByEmailForUpdate(authentication.getName())
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Authenticated user not found."
                        )
                );

        // STEP 2: Retrieve the invitation.
        Invitation invitation = invitationRepository
                .findById(invitationActionRequest.getInvitationId())
                .orElseThrow(() ->
                        new NoInvitationFoundException(
                                "Invitation not found."
                        )
                );

        // Only the invited student can respond.
        if (!invitation.getInvitedUser().getUserId()
                .equals(user.getUserId())) {

            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot respond to another student's invitation."
            );
        }

        // STEP 3: Lock the project.
        Project project = projectRepository
                .findByIdForUpdate(invitation.getProject().getProject_id())
                .orElseThrow(() ->
                        new NoSuchProjectsException(
                                "Project not found."
                        )
                );

        if (!project.getProject_id()
                .equals(invitationActionRequest.getProjectId())) {

            throw new IllegalStateException(
                    "Invitation does not belong to this project."
            );
        }

        // STEP 4: Check invitation status.
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException(
                    "This invitation has already been processed."
            );
        }

        String action = invitationActionRequest.getAction();

        if (action == null) {
            throw new IllegalArgumentException(
                    "Invitation action is required."
            );
        }

        // STEP 5: ACCEPT invitation.
        if ("ACCEPT".equalsIgnoreCase(action)) {

            // Check maximum group size.
            int memberCount =
                    projectMemberRepository.countByProjectId(
                            project.getProject_id()
                    );

            if (memberCount >= 3) {
                throw new ProjectMembersCountExceededException(
                        "This project already has 3 members."
                );
            }

            // Check whether the student belongs to
            // another project for the same subject.
            Integer subjectId = project.getProgrammeSubject()
                    .getSubject()
                    .getSubjectId();

            boolean alreadyParticipating =
                    projectMemberRepository.existsByUserAndSubject(
                            user.getUserId(),
                            subjectId
                    );

            if (alreadyParticipating) {
                throw new IllegalStateException(
                        "You already belong to a project for this subject."
                );
            }

            // STEP 6: Create the actual membership.
            ProjectMember projectMember = new ProjectMember();

            projectMember.setProject(project);
            projectMember.setUser(user);
            projectMember.setRole("MEMBER");
            projectMember.setJoinedAt(LocalDateTime.now());

            projectMemberRepository.saveAndFlush(projectMember);

            // STEP 7: Mark invitation as accepted.
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitation.setRespondedAt(LocalDateTime.now());

            invitationRepository.save(invitation);

            // STEP 8: Record project activity.
            ProjectActivities activity = new ProjectActivities();

            activity.setActivityType("MEMBER_ADDED");
            activity.setDescription(
                    user.getFullName()
                            + " joined the project."
            );
            activity.setCreatedAt(LocalDateTime.now());
            activity.setUser(user);
            activity.setProject(project);

            projectActivitiesRepository.save(activity);

            return "ACCEPTED";
        }

        // STEP 9: DECLINE invitation.
        else if ("DECLINE".equalsIgnoreCase(action)) {

            invitation.setStatus(InvitationStatus.REJECTED);
            invitation.setRespondedAt(LocalDateTime.now());

            invitationRepository.save(invitation);

            ProjectActivities activity = new ProjectActivities();

            activity.setActivityType("PROJECT_UPDATED");
            activity.setDescription(
                    user.getFullName()
                            + " declined the project invitation."
            );
            activity.setCreatedAt(LocalDateTime.now());
            activity.setUser(user);
            activity.setProject(project);

            projectActivitiesRepository.save(activity);

            return "REJECTED";
        }

        // STEP 10: Invalid action.
        else {
            throw new NoSuchMethodException(
                    "Invalid action. Use ACCEPT or DECLINE."
            );
        }
    }

}
