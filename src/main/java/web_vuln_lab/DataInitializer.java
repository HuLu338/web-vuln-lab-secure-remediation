package web_vuln_lab;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import web_vuln_lab.entity.FileRecord;
import web_vuln_lab.entity.User;
import web_vuln_lab.repository.FileRecordRepository;
import web_vuln_lab.repository.UserRepository;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository, FileRecordRepository fileRecordRepository) {
        return args -> {
            User user = userRepository.findByUsername("testuser").orElseGet(() -> {
                User newUser = new User();
                newUser.setUsername("testuser");
                newUser.setEmail("testuser@example.com");
                newUser.setPasswordHash("test-password-hash");
                newUser.setRole("USER");
                newUser.setCreatedAt(LocalDateTime.now());
                return userRepository.save(newUser);
            });

            if (fileRecordRepository.findAll().isEmpty()) {
                FileRecord file = new FileRecord();
                file.setOwner(user);
                file.setOriginalName("example.pdf");
                file.setStoredName("f1a2b3c4-example.pdf");
                file.setContentType("application/pdf");
                file.setSize(1024L);
                file.setStoragePath("/uploads/f1a2b3c4-example.pdf");
                file.setCreatedAt(LocalDateTime.now());

                fileRecordRepository.save(file);
            }
        };
    }
}