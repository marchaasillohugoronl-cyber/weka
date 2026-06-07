package com.streamflix.controlador;

import com.streamflix.servicio.EvaluacionServicio;
import com.streamflix.servicio.ExplorarServicio;
import com.streamflix.servicio.KnnServicio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Controlador para exploración del dataset, predicción KNN y evaluación con split
@Controller
public class ExplorarControlador {

    private final ExplorarServicio explorarServicio;
    private final KnnServicio      knnServicio;
    private final EvaluacionServicio evaluacionServicio;

    public ExplorarControlador(ExplorarServicio explorarServicio,
                                KnnServicio knnServicio,
                                EvaluacionServicio evaluacionServicio) {
        this.explorarServicio   = explorarServicio;
        this.knnServicio        = knnServicio;
        this.evaluacionServicio = evaluacionServicio;
    }

    // Vista principal: exploración + evaluación 80/20
    @GetMapping("/explorar")
    public String explorar(Model model) throws Exception {
        Map<String, Object> exploracion = explorarServicio.explorar();
        Map<String, Object> evaluacion  = evaluacionServicio.evaluarConSplit();
        model.addAttribute("exp",  exploracion);
        model.addAttribute("eval", evaluacion);
        return "explorar";
    }

    // Predicción KNN con parámetros del formulario
    @GetMapping("/explorar/predecir")
    public String predecir(
            @RequestParam(defaultValue = "25")      double edad,
            @RequestParam(defaultValue = "ACCION")  String categoria,
            @RequestParam(defaultValue = "4.0")     double puntuacion,
            @RequestParam(defaultValue = "SI")      String esFavorito,
            @RequestParam(defaultValue = "2")       double numCalificaciones,
            Model model) throws Exception {

        Map<String, Object> exploracion = explorarServicio.explorar();
        Map<String, Object> evaluacion  = evaluacionServicio.evaluarConSplit();
        Map<String, Object> prediccion  = knnServicio.predecirConKnn(
                edad, categoria, puntuacion, esFavorito, numCalificaciones);

        model.addAttribute("exp",       exploracion);
        model.addAttribute("eval",      evaluacion);
        model.addAttribute("prediccion", prediccion);
        return "explorar";
    }

    // Endpoint REST que retorna la exploración en JSON
    @GetMapping("/api/explorar")
    @ResponseBody
    public Map<String, Object> explorarApi() throws Exception {
        return explorarServicio.explorar();
    }

    // Endpoint REST que retorna la evaluación 80/20 en JSON
    @GetMapping("/api/explorar/evaluacion")
    @ResponseBody
    public Map<String, Object> evaluacionApi() throws Exception {
        return evaluacionServicio.evaluarConSplit();
    }
}
