package web_vuln_lab;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import web_vuln_lab.repository.FileRecordRepository;

@Controller
public class FilesController {

    private final FileRecordRepository fileRecordRepository;

    public FilesController(FileRecordRepository fileRecordRepository) {
        this.fileRecordRepository = fileRecordRepository;
    }

    @GetMapping("/files")
    public String files(Model model) {
        model.addAttribute("files", fileRecordRepository.findAll());
        return "files";
    }
}