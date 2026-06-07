package com.streamflix.controlador;

import com.streamflix.repositorio.UsuarioRepositorio;
import com.streamflix.servicio.RecomendacionServicio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador de recomendaciones.
 *
 * Endpoints REST:
 *   GET /api/recomendaciones/{usuarioId}          → lista de títulos
 *   GET /api/recomendaciones/{usuarioId}/detalle  → con confianza y metadata
 *   POST /api/recomendaciones/reentrenar          → fuerza reentrenamiento
 *
 * Endpoint Web (Thymeleaf):
 *   GET /recomendaciones/{usuarioId}              → vista HTML
 */
@Controller
public class RecomendacionControlador {

    private final RecomendacionServicio servicio;
    private final UsuarioRepositorio    usuarioRepo;

    public RecomendacionControlador(RecomendacionServicio servicio,
                                    UsuarioRepositorio usuarioRepo) {
        this.servicio    = servicio;
        this.usuarioRepo = usuarioRepo;
    }

    // ── REST ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/recomendaciones/{usuarioId}")
    @ResponseBody
    public List<String> recomendarApi(@PathVariable String usuarioId) throws Exception {
        return servicio.recomendar(usuarioId);
    }

    @GetMapping("/api/recomendaciones/{usuarioId}/detalle")
    @ResponseBody
    public List<Map<String, Object>> recomendarDetalle(@PathVariable String usuarioId) throws Exception {
        return servicio.recomendarConDetalle(usuarioId);
    }

    @PostMapping("/api/recomendaciones/reentrenar")
    @ResponseBody
    public Map<String, String> reentrenar() throws Exception {
        servicio.reentrenar();
        return Map.of("estado", "Modelo reentrenado correctamente");
    }

    @GetMapping("/api/recomendaciones/evaluar")
    @ResponseBody
    public Map<String, Object> evaluar() throws Exception {
        return servicio.evaluarModelo();
    }

    // ── Thymeleaf ─────────────────────────────────────────────────────────────

    /**
     * Vista HTML de recomendaciones para un usuario.
     * Usa recomendarConDetalle para mostrar confianza en las tarjetas.
     */
    @GetMapping("/recomendaciones/{usuarioId}")
    public String vistaRecomendaciones(@PathVariable String usuarioId, Model model) throws Exception {
        List<Map<String, Object>> detalle = servicio.recomendarConDetalle(usuarioId);

        // Obtener el nombre del usuario para mostrarlo en lugar del ID
        String nombreUsuario = usuarioRepo.findById(usuarioId)
                .map(u -> u.getNombre() != null ? u.getNombre() : usuarioId)
                .orElse(usuarioId);

        model.addAttribute("usuarioId",      usuarioId);
        model.addAttribute("nombreUsuario",  nombreUsuario);
        model.addAttribute("peliculas",      detalle);
        return "recomendaciones";
    }

    @GetMapping("/metricas")
    public String vistaMetricas(Model model) throws Exception {
        Map<String, Object> eval = servicio.evaluarModelo();
        model.addAttribute("eval", eval);
        return "metricas";
    }
}
