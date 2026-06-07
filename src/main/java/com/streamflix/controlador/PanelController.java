package com.streamflix.controlador;

import com.streamflix.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PanelController {

    private final UsuarioRepositorio usuarioRepo;

    public PanelController(UsuarioRepositorio usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        model.addAttribute("usuarios", usuarioRepo.findAll());
        return "panel";
    }
}
