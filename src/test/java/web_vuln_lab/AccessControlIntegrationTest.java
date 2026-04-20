package web_vuln_lab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import web_vuln_lab.entity.AuditLog;
import web_vuln_lab.entity.FileRecord;
import web_vuln_lab.entity.User;
import web_vuln_lab.repository.AuditLogRepository;
import web_vuln_lab.repository.FileRecordRepository;
import web_vuln_lab.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private User aliceUser;
    private User bobUser;
    private User adminUser;

    private FileRecord aliceFile;
    private FileRecord bobFile;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        aliceUser = new User();
        aliceUser.setUsername("alice_test_" + suffix);
        aliceUser.setEmail("alice_" + suffix + "@example.com");
        aliceUser.setPasswordHash("hash");
        aliceUser.setRole("USER");
        aliceUser.setCreatedAt(LocalDateTime.now());
        aliceUser = userRepository.save(aliceUser);

        bobUser = new User();
        bobUser.setUsername("bob_test_" + suffix);
        bobUser.setEmail("bob_" + suffix + "@example.com");
        bobUser.setPasswordHash("hash");
        bobUser.setRole("USER");
        bobUser.setCreatedAt(LocalDateTime.now());
        bobUser = userRepository.save(bobUser);

        adminUser = new User();
        adminUser.setUsername("admin_test_" + suffix);
        adminUser.setEmail("admin_" + suffix + "@example.com");
        adminUser.setPasswordHash("hash");
        adminUser.setRole("ADMIN");
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser = userRepository.save(adminUser);

        aliceFile = new FileRecord();
        aliceFile.setOwner(aliceUser);
        aliceFile.setOriginalName("alice-secret.txt");
        aliceFile.setStoredName("stored-alice-secret.txt");
        aliceFile.setContentType("text/plain");
        aliceFile.setSize(123L);
        aliceFile.setStoragePath("test/alice-secret.txt");
        aliceFile.setCreatedAt(LocalDateTime.now());
        aliceFile = fileRecordRepository.save(aliceFile);

        bobFile = new FileRecord();
        bobFile.setOwner(bobUser);
        bobFile.setOriginalName("bob-notes.txt");
        bobFile.setStoredName("stored-bob-notes.txt");
        bobFile.setContentType("text/plain");
        bobFile.setSize(456L);
        bobFile.setStoragePath("test/bob-notes.txt");
        bobFile.setCreatedAt(LocalDateTime.now());
        bobFile = fileRecordRepository.save(bobFile);
    }

    @Test
    void vulnerableFileEndpoint_allowsHorizontalUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/vuln/files/{id}", aliceFile.getId())
                        .param("user", bobUser.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceFile.getId()))
                .andExpect(jsonPath("$.ownerUsername").value(aliceUser.getUsername()))
                .andExpect(jsonPath("$.originalName").value("alice-secret.txt"));
    }

    @Test
    void secureFileEndpoint_deniesHorizontalUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/secure/files/{id}", aliceFile.getId())
                        .param("user", bobUser.getUsername()))
                .andExpect(status().isForbidden());
    }

    @Test
    void secureFileEndpoint_allowsOwnerAccess() throws Exception {
        mockMvc.perform(get("/secure/files/{id}", aliceFile.getId())
                        .param("user", aliceUser.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceFile.getId()))
                .andExpect(jsonPath("$.ownerUsername").value(aliceUser.getUsername()));
    }

    @Test
    void vulnerableAdminAuditEndpoint_allowsNonAdminAccess() throws Exception {
        mockMvc.perform(get("/vuln/admin/audit")
                        .param("user", bobUser.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("vulnerable"))
                .andExpect(jsonPath("$.requestedBy").value(bobUser.getUsername()))
                .andExpect(jsonPath("$.requestedRole").value("USER"));
    }

    @Test
    void secureAdminAuditEndpoint_deniesNonAdminAccess() throws Exception {
        mockMvc.perform(get("/secure/admin/audit")
                        .param("user", bobUser.getUsername()))
                .andExpect(status().isForbidden());
    }

    @Test
    void secureAdminAuditEndpoint_allowsAdminAccess() throws Exception {
        mockMvc.perform(get("/secure/admin/audit")
                        .param("user", adminUser.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("secure"))
                .andExpect(jsonPath("$.requestedBy").value(adminUser.getUsername()))
                .andExpect(jsonPath("$.requestedRole").value("ADMIN"));
    }

    @Test
    void deniedAndAllowedActions_areLogged() throws Exception {
        mockMvc.perform(get("/secure/files/{id}", aliceFile.getId())
                        .param("user", bobUser.getUsername()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/secure/admin/audit")
                        .param("user", adminUser.getUsername()))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();

        boolean deniedFileAccessLogged = logs.stream()
                .anyMatch(log -> "SECURE_FILE_ACCESS_DENIED".equals(log.getAction())
                        && log.getUser() != null
                        && bobUser.getUsername().equals(log.getUser().getUsername()));

        boolean allowedAdminAccessLogged = logs.stream()
                .anyMatch(log -> "SECURE_ADMIN_AUDIT_ALLOWED".equals(log.getAction())
                        && log.getUser() != null
                        && adminUser.getUsername().equals(log.getUser().getUsername()));

        assertThat(deniedFileAccessLogged).isTrue();
        assertThat(allowedAdminAccessLogged).isTrue();
    }
}