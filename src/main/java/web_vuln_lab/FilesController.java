package web_vuln_lab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
        User currentUser = userRepository.findByUsername(user)
                .orElseThrow(() -> new RuntimeException("User not found: " + user));

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

            User currentUser = userRepository.findByUsername(user)
                    .orElseThrow(() -> new RuntimeException("User not found: " + user));

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

    // 故意保留脆弱逻辑：只按文件 id 下载，不校验 owner
    @GetMapping("/files/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) throws IOException {
        FileRecord fileRecord = fileRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File record not found"));

        Path filePath = Paths.get(fileRecord.getStoragePath()).toAbsolutePath().normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("File not found or not readable");
        }

        String contentType = fileRecord.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileRecord.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // 故意保留脆弱逻辑：只按文件 id 删除，不校验 owner
    @PostMapping("/files/delete/{id}")
    public String deleteFile(@PathVariable Long id,
                             @RequestParam("user") String user) throws IOException {
        FileRecord fileRecord = fileRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File record not found"));

        Path filePath = Paths.get(fileRecord.getStoragePath()).toAbsolutePath().normalize();
        Files.deleteIfExists(filePath);

        fileRecordRepository.delete(fileRecord);

        return "redirect:/files?user=" + user;
    }
}