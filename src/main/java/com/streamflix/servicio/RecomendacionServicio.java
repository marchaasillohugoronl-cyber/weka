package com.streamflix.servicio;

import com.streamflix.modelo.*;
import com.streamflix.repositorio.*;

import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.*;

import weka.core.Instances;

/**
 * Motor de recomendaciones basado en RandomForest.
 *
 * Para cada película candidata calcula 5 features del perfil del usuario:
 *   1. edad
 *   2. categoria de la película
 *   3. puntuación promedio del usuario en ese género (fallback 3.0)
 *   4. esFavorito: si el género está en generosFavoritos
 *   5. numCalificaciones: cuántas películas de ese género ha calificado
 *
 * La feature numCalificaciones es clave para la confianza:
 * con más historial en el género, el modelo produce probabilidades más extremas y fiables.
 */
@Service
public class RecomendacionServicio {

    private final UsuarioRepositorio usuarioRepo;
    private final PeliculaRepositorio peliculaRepo;
    private final CalificacionRepositorio calificacionRepo;
    private final WekaServicio weka;
    private final DatasetServicio dataset;

    public RecomendacionServicio(
            UsuarioRepositorio usuarioRepo,
            PeliculaRepositorio peliculaRepo,
            CalificacionRepositorio calificacionRepo,
            WekaServicio weka,
            DatasetServicio dataset) {

        this.usuarioRepo = usuarioRepo;
        this.peliculaRepo = peliculaRepo;
        this.calificacionRepo = calificacionRepo;
        this.weka = weka;
        this.dataset = dataset;
    }

    // ── API pública ──────────────────────────────────────────────────────────

    public List<String> recomendar(String usuarioId) throws Exception {
        return recomendarConDetalle(usuarioId).stream()
                .filter(m -> "SI".equals(m.get("recomendar")))
                .map(m -> (String) m.get("titulo"))
                .toList();
    }

    /**
     * Devuelve cada película candidata con predicción y confianza del RandomForest.
     */
    public List<Map<String, Object>> recomendarConDetalle(String usuarioId) throws Exception {

        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + usuarioId));

        List<Pelicula> candidatas = peliculaRepo.findPeliculasNoVistasPorUsuario(usuarioId);

        asegurarModeloEntrenado();

        double edad         = usuario.getEdad() != null ? usuario.getEdad() : 25;
        double promedioGlobal = Optional.ofNullable(
                calificacionRepo.promedioGlobalUsuario(usuarioId)).orElse(3.0);

        // Pre-cargar calificaciones del usuario una sola vez
        List<Calificacion> calificacionesUsuario = calificacionRepo.findByUsuarioId(usuarioId);

        List<Map<String, Object>> resultados = new ArrayList<>();

        for (Pelicula p : candidatas) {

            String categoria = dataset.normalizarCategoria(p.getCategoria());
            if (categoria == null) continue;

            double puntuacion       = promedioCategoria(calificacionesUsuario, categoria, promedioGlobal);
            String esFavorito       = dataset.generoEsFavorito(usuario.getGenerosFavoritos(), categoria);
            double numCalificaciones = contarCalificacionesEnGenero(calificacionesUsuario, categoria);

            String prediccion = weka.predecir(edad, categoria, puntuacion, esFavorito, numCalificaciones);
            double[] dist     = weka.probabilidadPrediccion(edad, categoria, puntuacion, esFavorito, numCalificaciones);

            // dist[0] = P(SI) según el orden del ARFF (@attribute recomendar {SI,NO})
            int confianzaPct = (int) Math.round((dist.length > 0 ? dist[0] : 0.5) * 100);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("titulo",            p.getTitulo());
            item.put("categoria",         p.getCategoria());
            item.put("anio",              p.getAnio());
            item.put("recomendar",        prediccion);
            item.put("confianza",         confianzaPct + "%");
            item.put("esFavorito",        esFavorito);
            item.put("numCalificaciones", (int) numCalificaciones);
            resultados.add(item);
        }

        // Ordenar: mayor confianza arriba (independiente de SI/NO)
        resultados.sort((a, b) -> {
            int ca = Integer.parseInt(((String) a.get("confianza")).replace("%", ""));
            int cb = Integer.parseInt(((String) b.get("confianza")).replace("%", ""));
            return Integer.compare(cb, ca);
        });

        return resultados;
    }

    public void reentrenar() throws Exception {
        weka.resetear();
        asegurarModeloEntrenado();
    }

    /**
     * Ejecuta 10-fold cross-validation sobre el dataset actual
     * y devuelve métricas reales: accuracy, precision, recall, F1, kappa,
     * error medio y matriz de confusión.
     */
    public Map<String, Object> evaluarModelo() throws Exception {
        String arff = dataset.generarARFF();
        Instances data = new Instances(new StringReader(arff));
        return weka.evaluarConCrossValidation(data, 10);
    }

    // ── Privado ──────────────────────────────────────────────────────────────

    private void asegurarModeloEntrenado() throws Exception {
        if (!weka.listo()) {
            String arff = dataset.generarARFF();
            Instances data = new Instances(new StringReader(arff));
            weka.entrenar(data);
        }
    }

    private double promedioCategoria(List<Calificacion> calificaciones,
                                     String categoria, double fallback) {
        OptionalDouble promedio = calificaciones.stream()
                .filter(c -> peliculaRepo.findById(c.getPeliculaId())
                        .map(p -> categoria.equalsIgnoreCase(dataset.normalizarCategoria(p.getCategoria())))
                        .orElse(false))
                .mapToInt(Calificacion::getPuntuacion)
                .average();
        return promedio.orElse(fallback);
    }

    private double contarCalificacionesEnGenero(List<Calificacion> calificaciones,
                                                String categoria) {
        long count = calificaciones.stream()
                .filter(c -> peliculaRepo.findById(c.getPeliculaId())
                        .map(p -> categoria.equalsIgnoreCase(dataset.normalizarCategoria(p.getCategoria())))
                        .orElse(false))
                .count();
        return Math.min(count, 10); // cap en 10, valores mayores no añaden señal
    }
}
