package web_vuln_lab;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import web_vuln_lab.entity.Comment;
import web_vuln_lab.entity.User;
import web_vuln_lab.repository.CommentRepository;
import web_vuln_lab.repository.UserRepository;

import java.time.LocalDateTime;

@Controller
public class CommentsController {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentsController(CommentRepository commentRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/comments")
    public String comments(@RequestParam(defaultValue = "alice") String user, Model model) {
        User currentUser = userRepository.findByUsername(user)
                .orElseThrow(() -> new RuntimeException("User not found: " + user));

        model.addAttribute("currentUser", currentUser.getUsername());
        model.addAttribute("comments", commentRepository.findAllByOrderByCreatedAtDesc());
        return "comments";
    }

    @PostMapping("/comments")
    public String addComment(@RequestParam("user") String user,
                             @RequestParam("content") String content,
                             Model model) {
        User currentUser = userRepository.findByUsername(user)
                .orElseThrow(() -> new RuntimeException("User not found: " + user));

        if (content == null || content.isBlank()) {
            model.addAttribute("currentUser", currentUser.getUsername());
            model.addAttribute("comments", commentRepository.findAllByOrderByCreatedAtDesc());
            model.addAttribute("errorMessage", "Comment cannot be empty.");
            return "comments";
        }

        if (content.length() > 500) {
            model.addAttribute("currentUser", currentUser.getUsername());
            model.addAttribute("comments", commentRepository.findAllByOrderByCreatedAtDesc());
            model.addAttribute("errorMessage", "Comment is too long. Maximum length is 500 characters.");
            return "comments";
        }

        Comment comment = new Comment();
        comment.setUser(currentUser);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());

        commentRepository.save(comment);

        return "redirect:/comments?user=" + user;
    }
}