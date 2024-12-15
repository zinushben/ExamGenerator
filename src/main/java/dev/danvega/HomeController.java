package dev.danvega;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        String exampleContent = "Welcome to the mock exam generator! Send a POST request to '/generate' with your course content as plain text.";
        model.addAttribute("exampleContent", exampleContent);
        return "index";
    }
}
