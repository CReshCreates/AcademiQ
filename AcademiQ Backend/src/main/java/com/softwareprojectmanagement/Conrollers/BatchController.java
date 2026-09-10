
package com.softwareprojectmanagement.Conrollers;

import com.softwareprojectmanagement.DTO.Response.Registration.AvailableBatches;
import com.softwareprojectmanagement.Models.Batch;
import com.softwareprojectmanagement.Services.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchController {
    private final BatchService batchService;

    @GetMapping("/getAvailableBatches")
    public ResponseEntity<List<AvailableBatches>> getAllActiveBatches(){
        return ResponseEntity.status(HttpStatus.OK).body(batchService.activeBatch());
    }
}
