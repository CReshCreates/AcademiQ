package com.softwareprojectmanagement.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadPath;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {

        this.uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory.", e);
        }
    }

    public String storeProposalPdf(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("PDF file is required.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null ||
                !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("Only PDF files are allowed.");
        }

        String filename = UUID.randomUUID() + ".pdf";

        Path destination = uploadPath
                .resolve("proposals")
                .normalize();

        try {
            Files.createDirectories(destination);

            Path targetFile = destination.resolve(filename).normalize();

            file.transferTo(targetFile);

            return "/uploads/proposals/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Could not store PDF file.", e);
        }
    }
}