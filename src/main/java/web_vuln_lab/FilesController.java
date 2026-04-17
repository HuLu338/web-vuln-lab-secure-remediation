package web_vuln_lab;

import jakarta.servlet.http.HttpServletRequest;
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

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String[] ALLOWED_EXTENSIONS = {".txt", ".pdf", ".png", ".jpg", ".jpeg"};
    
    private final FileRecordRepository fileRecordRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final FileAccessService fileAccessService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public FilesController(FileRecordRepository fileRecordRepository,
                       UserRepository userRepository,
                       AuditLogService auditLogService,
                       FileAccessService fileAccessService) {
    this.fileRecordRepository = fileRecordRepository;
    this.userRepository = userRepository;
    this.auditLogService = auditLogService;
    this.fileAccessService = fileAccessService;
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
                             @RequestParam("user") String user,
                             Model model,
                             HttpServletRequest request) {
        try {
            User currentUser = getCurrentUser(user);

            if (file.isEmpty()) {
                model.addAttribute("currentUser", currentUser.getUsername());
                model.addAttribute("files", fileRecordRepository.findByOwner_Id(currentUser.getId()));
                model.addAttribute("errorMessage", "Please choose a file.");
                return "files";
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                model.addAttribute("currentUser", currentUser.getUsername());
                model.addAttribute("files", fileRecordRepository.findByOwner_Id(currentUser.getId()));
                model.addAttribute("errorMessage", "Invalid file name.");
                return "files";
            }

            if (!isAllowedExtension(originalName)) {
                model.addAttribute("currentUser", currentUser.getUsername());
                model.addAttribute("files", fileRecordRepository.findByOwner_Id(currentUser.getId()));
                model.addAttribute("errorMessage", "File type not allowed. Only txt, pdf, png, jpg, jpeg are allowed.");
                return "files";
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                model.addAttribute("currentUser", currentUser.getUsername());
                model.addAttribute("files", fileRecordRepository.findByOwner_Id(currentUser.getId()));
                model.addAttribute("errorMessage", "File is too large. Maximum size is 2MB.");
                return "files";
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

            auditLogService.log(
                    currentUser,
                    "UPLOAD_FILE",
                    "FILE",
                    fileRecord.getId(),
                    request.getRemoteAddr()
            );

            return "redirect:/files?user=" + user;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }

    @GetMapping("/files/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id,
                                                 @RequestParam("user") String user,
                                                 HttpServletRequest request) {
        try {
            User currentUser = getCurrentUser(user);
            FileRecord fileRecord = getAuthorizedFile(currentUser, id, request.getRemoteAddr(), "DOWNLOAD_FILE_DENIED");

            Path filePath = Paths.get(fileRecord.getStoragePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found or not readable: " + filePath);
            }

            auditLogService.log(
                    currentUser,
                    "DOWNLOAD_FILE",
                    "FILE",
                    fileRecord.getId(),
                    request.getRemoteAddr()
            );

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
                             @RequestParam("user") String user,
                             HttpServletRequest request) throws IOException {
        User currentUser = getCurrentUser(user);
        FileRecord fileRecord = getAuthorizedFile(currentUser, id, request.getRemoteAddr(), "DELETE_FILE_DENIED");

        Path filePath = Paths.get(fileRecord.getStoragePath()).toAbsolutePath().normalize();
        Files.deleteIfExists(filePath);

        fileRecordRepository.delete(fileRecord);

        auditLogService.log(
                currentUser,
                "DELETE_FILE",
                "FILE",
                id,
                request.getRemoteAddr()
        );

        return "redirect:/files?user=" + user;
    }

@GetMapping("/vuln/files/{id}")
@ResponseBody
public FileResponseDto getFileVulnerable(@PathVariable Long id,
                                         @RequestParam(defaultValue = "alice") String user) {
    getCurrentUser(user); // 只检查用户是否存在

    FileRecord file = fileAccessService.getFileVulnerable(id);
    return toDto(file);
}

@GetMapping("/secure/files/{id}")
@ResponseBody
public FileResponseDto getFileSecure(@PathVariable Long id,
                                     @RequestParam(defaultValue = "alice") String user) {
    User currentUser = getCurrentUser(user);
    FileRecord file = fileAccessService.getFileSecure(id, currentUser);
    return toDto(file);
}

private FileResponseDto toDto(FileRecord file) {
    return new FileResponseDto(
            file.getId(),
            file.getOriginalName(),
            file.getStoredName(),
            file.getContentType(),
            file.getSize(),
            file.getOwner().getId(),
            file.getOwner().getUsername()
    );
}


    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + username));
    }

    private FileRecord getAuthorizedFile(User currentUser,
                                         Long fileId,
                                         String ip,
                                         String deniedAction) {
        FileRecord fileRecord = fileRecordRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "File record not found: " + fileId));

        if (!fileRecord.getOwner().getId().equals(currentUser.getId())) {
            auditLogService.log(
                    currentUser,
                    deniedAction,
                    "FILE",
                    fileId,
                    ip
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access this file");
        }

        return fileRecord;
    }

    private boolean isAllowedExtension(String filename) {
        String lower = filename.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}