package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Request.Invitation.InvitationActionRequest;
import com.softwareprojectmanagement.DTO.Request.Invitation.InvitationRequest;
import com.softwareprojectmanagement.DTO.Response.Invitation.MyInvitationsResponse;
import com.softwareprojectmanagement.Services.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invitation")
@RequiredArgsConstructor
public class InvitationController {
    private final InvitationService invitationService;

    @PostMapping("/inviteMember")
    public ResponseEntity<Integer> inviteToProject(@RequestBody InvitationRequest invitationRequest){
        return ResponseEntity.ok(invitationService.inviteToProject(invitationRequest));
    }

    @GetMapping("/invitationNotification")
    public ResponseEntity<List<MyInvitationsResponse>> getMyInvitations(){
        return ResponseEntity.status(HttpStatus.OK).body(invitationService.getMyProjectsInvitation());
    }

    @PostMapping("/invitationAction")
    public ResponseEntity<String> invitationAction(@RequestBody InvitationActionRequest invitationActionRequest) throws NoSuchMethodException {
        return ResponseEntity.status(HttpStatus.OK).body(invitationService.invitationAction(invitationActionRequest));
    }
}
