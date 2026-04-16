package web_vuln_lab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import web_vuln_lab.entity.FileRecord;
import web_vuln_lab.entity.User;
import web_vuln_lab.repository.FileRecordRepository;
import web_vuln_lab.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class FilesController {

    private final FileRecordRepository fileRecordRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public FilesController(FileRecordRepository fileRecordRepository, UserRepository userRepository) {
        this.fileRecordRepository = fileRecordRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/files")
    public String files(@RequestParam(defaultValue = "alice") String user, Model model) {
        User currentUser = getCurrentUser(user);

        model.addAttribute("currentUser", currentUser.getUsername());
        model.addAttribute("files", fileRecordRepository.findByOwner_Id(currentUser.getId()));
        return "files";
    }

    @PostMapping("/files/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam("user") String user) {
        try {
            if (file.isEmpty()) {
                return "redirect:/files?user=" + user;
            }

            User currentUser = getCurrentUser(user);

            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                originalName = "unknown-file";
            }

            String storedName = UUID.randomUUID() + "-" + originalName;

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path targetPath = uploadPath.resolve(storedName);
            file.transferTo(targetPath.toFile());

            FileRecord fileRecord = new FileRecord();
            fileRecord.setOwner(currentUser);
            fileRecord.setOriginalName(originalName);
            fileRecord.setStoredName(storedName);
            fileRecord.setContentType(file.getContentType());
            fileRecord.setSize(file.getSize());
            fileRecord.setStoragePath(targetPath.toString());
            fileRecord.setCreatedAt(LocalDateTime.now());

            fileRecordRepository.save(fileRecord);

            return "redirect:/files?user=" + user;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }

    @GetMapping("/files/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id,
                                                 @RequestParam("user") String user) {
        try {
            User currentUser = getCurrentUser(user);
            FileRecord fileRecord = getAuthorizedFile(currentUser, id);

            Path filePath = Paths.get(fileRecord.getStoragePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found or not readable: " + filePath);
            }

            String downloadName = fileRecord.getOriginalName();
            if (downloadName == null || downloadName.isBlank()) {
                downloadName = "download-file";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + downloadName.replace("\"", "") + "\"")
                    .body(resource);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Download failed: " + e.getMessage(), e);
        }
    }

    @PostMapping("/files/delete/{id}")
    public String deleteFile(@PathVariable Long id,
                             @RequestParam("user") String user) throws IOException {
        User currentUser = getCurrentUser(user);
        FileRecord fileRecord = getAuthorizedFile(currentUser, id);

        Path filePath = Paths.get(fileRecord.getStoragePath()).toAbsolutePath().normalize();
        Files.deleteIfExists(filePath);

        fileRecordRepository.delete(fileRecord);

        return "redirect:/files?user=" + user;
    }

    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + username));
    }

    private FileRecord getAuthorizedFile(User currentUser, Long fileId) {
        FileRecord fileRecord = fileRecordRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "File record not found: " + fileId));

        if (!fileRecord.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access this file");
        }

        return fileRecord;
    }
}