package web_vuln_lab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import web_vuln_lab.entity.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByUser_Id(Long userId);

    List<Comment> findAllByOrderByCreatedAtDesc();
}