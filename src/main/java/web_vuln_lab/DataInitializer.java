package web_vuln_lab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import web_vuln_lab.entity.FileRecord;
import web_vuln_lab.entity.User;
import web_vuln_lab.repository.FileRecordRepository;
import web_vuln_lab.repository.UserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Bean
    CommandLineRunner initData(UserRepository userRepository, FileRecordRepository fileRecordRepository) {
        return args -> {
            User alice = userRepository.findByUsername("alice").orElseGet(() -> {
                User user = new User();
                user.setUsername("alice");
                user.setEmail("alice@example.com");
                user.setPasswordHash("alice-password-hash");
                user.setRole("USER");
                user.setCreatedAt(LocalDateTime.now());
                return userRepository.save(user);
            });

            User bob = userRepository.findByUsername("bob").orElseGet(() -> {
                User user = new User();
                user.setUsername("bob");
                user.setEmail("bob@example.com");
                user.setPasswordHash("bob-password-hash");
                user.setRole("USER");
                user.setCreatedAt(LocalDateTime.now());
                return userRepository.save(user);
            });

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            if (fileRecordRepository.findByOwner_Id(alice.getId()).isEmpty()) {
                String storedName = "alice-demo.txt";
                Path filePath = uploadPath.resolve(storedName);
                Files.writeString(filePath, "This is Alice's demo file.");

                FileRecord file = new FileRecord();
                file.setOwner(alice);
                file.setOriginalName("alice-demo.txt");
                file.setStoredName(storedName);
                file.setContentType("text/plain");
                file.setSize(Files.size(filePath));
                file.setStoragePath(filePath.toString());
                file.setCreatedAt(LocalDateTime.now());
                fileRecordRepository.save(file);
            }

            if (fileRecordRepository.findByOwner_Id(bob.getId()).isEmpty()) {
                String storedName = "bob-demo.txt";
                Path filePath = uploadPath.resolve(storedName);
                Files.writeString(filePath, "This is Bob's demo file.");

                FileRecord file = new FileRecord();
                file.setOwner(bob);
                file.setOriginalName("bob-demo.txt");
                file.setStoredName(storedName);
                file.setContentType("text/plain");
                file.setSize(Files.size(filePath));
                file.setStoragePath(filePath.toString());
                file.setCreatedAt(LocalDateTime.now());
                fileRecordRepository.save(file);
            }
        };
    }
}