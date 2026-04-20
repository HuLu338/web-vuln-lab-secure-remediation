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
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogRepository auditLogRepository,
                          UserRepository userRepository,
                          AuthorizationService authorizationService,
                          AuditLogService auditLogService) {
    this.auditLogRepository = auditLogRepository;
    this.userRepository = userRepository;
    this.authorizationService = authorizationService;
    this.auditLogService = auditLogService;
}

    @GetMapping("/audit-logs")
    public String auditLogs(Model model) {
        model.addAttribute("logs", auditLogRepository.findAllByOrderByCreatedAtDesc());
        return "audit-logs";
    }

    @GetMapping("/vuln/admin/audit")
@ResponseBody
public Map<String, Object> getAdminAuditVulnerable(
        @RequestParam(defaultValue = "testuser") String user,
        jakarta.servlet.http.HttpServletRequest request) {

    User currentUser = getCurrentUser(user);

    auditLogService.log(
            currentUser,
            "VULN_ADMIN_AUDIT_ACCESS",
            "AUDIT_LOG",
            null,
            request.getRemoteAddr()
    );

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
        @RequestParam(defaultValue = "testuser") String user,
        jakarta.servlet.http.HttpServletRequest request) {

    User currentUser = getCurrentUser(user);

    if (!authorizationService.isAdmin(currentUser)) {
        auditLogService.log(
                currentUser,
                "SECURE_ADMIN_AUDIT_DENIED",
                "AUDIT_LOG",
                null,
                request.getRemoteAddr()
        );
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    auditLogService.log(
            currentUser,
            "SECURE_ADMIN_AUDIT_ALLOWED",
            "AUDIT_LOG",
            null,
            request.getRemoteAddr()
    );

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

    @GetMapping("/security-events")
public String securityEvents(Model model) {
    var eventRows = auditLogRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(log -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", log.getId());
                row.put("createdAt", log.getCreatedAt());
                row.put("username", log.getUser() != null ? log.getUser().getUsername() : "N/A");
                row.put("action", log.getAction());
                row.put("targetType", log.getTargetType());
                row.put("targetId", log.getTargetId());
                row.put("ip", log.getIp());
                return row;
            })
            .toList();

    model.addAttribute("logs", eventRows);
    return "security-events";
}

}