package web_vuln_lab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import web_vuln_lab.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}