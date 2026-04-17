package web_vuln_lab;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import web_vuln_lab.entity.FileRecord;
import web_vuln_lab.entity.User;
import web_vuln_lab.repository.FileRecordRepository;

@Service
public class FileAccessService {

    private final FileRecordRepository fileRecordRepository;
    private final AuthorizationService authorizationService;

    public FileAccessService(FileRecordRepository fileRecordRepository,
                             AuthorizationService authorizationService) {
        this.fileRecordRepository = fileRecordRepository;
        this.authorizationService = authorizationService;
    }

    // 漏洞版：仅凭文件 ID 直接读取
    public FileRecord getFileVulnerable(Long fileId) {
        return fileRecordRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    // 修复版：检查对象级授权
    public FileRecord getFileSecure(Long fileId, User currentUser) {
        FileRecord file = fileRecordRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (!authorizationService.canAccessFile(currentUser, file)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return file;
    }
}