package com.softwareprojectmanagement.Services;


import com.softwareprojectmanagement.Exceptions.NoActiveBatchException;
import com.softwareprojectmanagement.Models.Batch;
import com.softwareprojectmanagement.Repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchService {
    private final BatchRepository batchRepository;

    public List<Batch> activeBatch(){
        List<Batch> batchList = batchRepository.findAllByStatus("ACTIVE");

        if(batchList.isEmpty()){
            throw new NoActiveBatchException("There are no active batches currently!!!");
        }

        return batchList;
    }
}
