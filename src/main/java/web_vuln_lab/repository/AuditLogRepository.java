package web_vuln_lab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import web_vuln_lab.entity.AuditLog;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUser_Id(Long userId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}