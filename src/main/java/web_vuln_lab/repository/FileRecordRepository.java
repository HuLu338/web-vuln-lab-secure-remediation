package web_vuln_lab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import web_vuln_lab.entity.FileRecord;

import java.util.List;

public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {

    List<FileRecord> findByOwner_Id(Long ownerId);
}