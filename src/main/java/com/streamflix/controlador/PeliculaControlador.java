package com.streamflix.controlador;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/peliculas")
public class PeliculaControlador {

    @GetMapping
    public String listar() {
        return "Listado de peliculas";
    }
}
