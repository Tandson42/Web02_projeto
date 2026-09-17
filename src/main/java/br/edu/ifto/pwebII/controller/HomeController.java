package br.edu.ifto.pwebII.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {

    // carrega o menu principal na raiz da aplicação
    @GetMapping("/")
    public ModelAndView menu() {
        return new ModelAndView("menu");
    }
}