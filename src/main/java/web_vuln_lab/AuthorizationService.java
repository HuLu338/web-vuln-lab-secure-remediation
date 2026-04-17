package web_vuln_lab;

import org.springframework.stereotype.Service;
import web_vuln_lab.entity.FileRecord;
import web_vuln_lab.entity.User;

@Service
public class AuthorizationService {

    public boolean canAccessFile(User currentUser, FileRecord file) {
        if (currentUser == null || file == null) {
            return false;
        }

        if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return true;
        }

        if (file.getOwner() == null || file.getOwner().getId() == null) {
            return false;
        }

        return file.getOwner().getId().equals(currentUser.getId());
    }
    public boolean isAdmin(User currentUser) {
    return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
}
}