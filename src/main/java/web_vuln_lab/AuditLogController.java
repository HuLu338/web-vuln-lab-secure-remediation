package web_vuln_lab;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import web_vuln_lab.entity.User;
import web_vuln_lab.repository.AuditLogRepository;
import web_vuln_lab.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

    public AuditLogController(AuditLogRepository auditLogRepository,
                              UserRepository userRepository,
                              AuthorizationService authorizationService) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/audit-logs")
    public String auditLogs(Model model) {
        model.addAttribute("logs", auditLogRepository.findAllByOrderByCreatedAtDesc());
        return "audit-logs";
    }

    @GetMapping("/vuln/admin/audit")
    @ResponseBody
    public Map<String, Object> getAdminAuditVulnerable(
            @RequestParam(defaultValue = "testuser") String user) {

        User currentUser = getCurrentUser(user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "vulnerable");
        response.put("requestedBy", currentUser.getUsername());
        response.put("requestedRole", currentUser.getRole());
        response.put("totalAuditLogs", auditLogRepository.count());

        return response;
    }

    @GetMapping("/secure/admin/audit")
    @ResponseBody
    public Map<String, Object> getAdminAuditSecure(
            @RequestParam(defaultValue = "testuser") String user) {

        User currentUser = getCurrentUser(user);

        if (!authorizationService.isAdmin(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "secure");
        response.put("requestedBy", currentUser.getUsername());
        response.put("requestedRole", currentUser.getRole());
        response.put("totalAuditLogs", auditLogRepository.count());

        return response;
    }

    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + username));
    }
}