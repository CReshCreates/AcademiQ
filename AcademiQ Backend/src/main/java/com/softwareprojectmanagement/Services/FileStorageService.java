
package com.softwareprojectmanagement.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_PDF_SIZE =
            10L * 1024 * 1024;

    private static final byte[] PDF_SIGNATURE =
            "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final Path uploadPath;

    public FileStorageService(
            @Value("${file.upload-dir}") String uploadDir
    ) {

        this.uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not create upload directory.", e
            );
        }
    }


    // ==========================================
    // INITIAL PROJECT PROPOSAL
    // ==========================================

    public String storeProposalPdf(MultipartFile file) {

        return storePdf(file, "proposals");
    }


    // ==========================================
    // MILESTONE SUBMISSION OR RESUBMISSION
    // ==========================================

    public String storeMilestonePdf(MultipartFile file) {

        return storePdf(file, "milestones");
    }


    // ==========================================
    // SHARED PDF STORAGE
    // ==========================================

    private String storePdf(
            MultipartFile file,
            String directoryName
    ) {

        validatePdf(file);

        String filename = UUID.randomUUID() + ".pdf";

        Path destination = uploadPath
                .resolve(directoryName)
                .normalize();

        if (!destination.startsWith(uploadPath)) {
            throw new IllegalStateException(
                    "Invalid upload directory."
            );
        }

        Path targetFile = destination
                .resolve(filename)
                .normalize();

        if (!targetFile.startsWith(destination)) {
            throw new IllegalStateException(
                    "Invalid upload destination."
            );
        }

        try {
            Files.createDirectories(destination);

            // No REPLACE_EXISTING option:
            // existing files cannot be overwritten.
            try (InputStream inputStream =
                         file.getInputStream()) {

                Files.copy(inputStream, targetFile);
            }

            return "/uploads/"
                    + directoryName
                    + "/"
                    + filename;

        } catch (IOException e) {

            // Remove a partially written file, if any.
            try {
                Files.deleteIfExists(targetFile);
            } catch (IOException cleanupException) {
                e.addSuppressed(cleanupException);
            }

            throw new IllegalStateException(
                    "Could not store PDF file.", e
            );
        }
    }


    // ==========================================
    // VALIDATE PDF BEFORE STORAGE
    // ==========================================

    private void validatePdf(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A PDF file is required."
            );
        }

        if (file.getSize() > MAX_PDF_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "PDF file must not exceed 10 MB."
            );
        }

        String originalFilename =
                file.getOriginalFilename();

        if (originalFilename == null
                || !originalFilename.toLowerCase(
                Locale.ROOT
        ).endsWith(".pdf")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PDF files are allowed."
            );
        }

        try (InputStream inputStream =
                     file.getInputStream()) {

            byte[] header =
                    inputStream.readNBytes(
                            PDF_SIGNATURE.length
                    );

            if (header.length != PDF_SIGNATURE.length) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid PDF file."
                );
            }

            for (int i = 0;
                 i < PDF_SIGNATURE.length;
                 i++) {

                if (header[i] != PDF_SIGNATURE[i]) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Invalid PDF file content."
                    );
                }
            }

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Could not validate PDF file.", e
            );
        }
    }
}