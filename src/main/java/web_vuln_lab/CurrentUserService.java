package web_vuln_lab;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import web_vuln_lab.entity.User;
import web_vuln_lab.repository.UserRepository;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        String username = authentication.getName();
        if ("anonymousUser".equals(username)) {
            return null;
        }

        return userRepository.findByUsername(username).orElse(null);
    }
}