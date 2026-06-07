package com.streamflix.controlador;

import com.streamflix.modelo.Pelicula;
import com.streamflix.modelo.Usuario;
import com.streamflix.repositorio.PeliculaRepositorio;
import com.streamflix.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
public class WebControlador {

    private final UsuarioRepositorio usuarioRepo;
    private final PeliculaRepositorio peliculaRepo;

    public WebControlador(UsuarioRepositorio usuarioRepo, PeliculaRepositorio peliculaRepo) {
        this.usuarioRepo = usuarioRepo;
        this.peliculaRepo = peliculaRepo;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/diagramas")
    public String diagramas() {
        return "diagramas";
    }

    // ── Usuarios ─────────────────────────────────────────────────────────────

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepo.findAll());
        return "usuarios";
    }

    @PostMapping("/usuarios")
    public String guardarUsuario(
            @RequestParam(required = false) String id,
            @RequestParam String nombre,
            @RequestParam(required = false) Integer edad,
            @RequestParam(required = false) String pais,
            @RequestParam(required = false) String generosFavoritos) {

        Usuario u = new Usuario();
        u.setId((id != null && !id.isBlank()) ? id : UUID.randomUUID().toString().substring(0, 8));
        u.setNombre(nombre);
        u.setEdad(edad);
        u.setPais(pais);
        u.setGenerosFavoritos(generosFavoritos != null ? generosFavoritos.toUpperCase() : null);
        usuarioRepo.save(u);
        return "redirect:/usuarios";
    }

    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable String id) {
        usuarioRepo.deleteById(id);
        return "redirect:/usuarios";
    }

    // ── Películas ─────────────────────────────────────────────────────────────

    @GetMapping("/peliculas")
    public String listarPeliculas(Model model) {
        model.addAttribute("peliculas", peliculaRepo.findAll());
        return "peliculas";
    }

    @PostMapping("/peliculas")
    public String guardarPelicula(
            @RequestParam String id,
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam String categoria,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer duracionMinutos) {

        Pelicula p = new Pelicula();
        p.setId(id);
        p.setTitulo(titulo);
        p.setDescripcion(descripcion);
        p.setCategoria(categoria.toUpperCase());
        p.setAnio(anio);
        p.setDuracionMinutos(duracionMinutos);
        peliculaRepo.save(p);
        return "redirect:/peliculas";
    }

    @GetMapping("/peliculas/eliminar/{id}")
    public String eliminarPelicula(@PathVariable String id) {
        peliculaRepo.deleteById(id);
        return "redirect:/peliculas";
    }
}
