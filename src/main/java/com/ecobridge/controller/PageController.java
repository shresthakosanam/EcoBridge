package com.ecobridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/login") public String login() { return "forward:/login.html"; }
    @GetMapping("/about") public String about() { return "forward:/about.html"; }
    @GetMapping("/dashboard") public String dashboard() { return "redirect:/dashboard.html"; }
    @GetMapping("/eco-pickup") public String pickup() { return "redirect:/pickup.html"; }
    @GetMapping("/eco-events") public String events() { return "redirect:/events.html"; }
    @GetMapping("/eco-feed") public String feed() { return "redirect:/feed.html"; }
}
