package com.streamflix.servicio;

import com.streamflix.modelo.Calificacion;
import com.streamflix.modelo.Pelicula;
import com.streamflix.modelo.Usuario;
import com.streamflix.repositorio.CalificacionRepositorio;
import com.streamflix.repositorio.PeliculaRepositorio;
import com.streamflix.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

// Genera el dataset ARFF con datos estáticos + dinámicos de la BD
@Service
public class DatasetServicio {

    private static final Set<String> CATEGORIAS_VALIDAS = Set.of(
            "ACCION", "ROMANCE", "TERROR", "COMEDIA", "CIENCIA_FICCION"
    );

    private final UsuarioRepositorio usuarioRepo;
    private final PeliculaRepositorio peliculaRepo;
    private final CalificacionRepositorio calificacionRepo;

    public DatasetServicio(UsuarioRepositorio usuarioRepo,
                           PeliculaRepositorio peliculaRepo,
                           CalificacionRepositorio calificacionRepo) {
        this.usuarioRepo      = usuarioRepo;
        this.peliculaRepo     = peliculaRepo;
        this.calificacionRepo = calificacionRepo;
    }

    // Genera el ARFF combinando datos estáticos + datos reales de la BD
    public String generarARFF() {
        StringBuilder sb = new StringBuilder();

        sb.append("@relation streamflix\n\n");
        sb.append("@attribute edad numeric\n");
        sb.append("@attribute categoria {ACCION,ROMANCE,TERROR,COMEDIA,CIENCIA_FICCION}\n");
        sb.append("@attribute puntuacion numeric\n");
        sb.append("@attribute esFavorito {SI,NO}\n");
        sb.append("@attribute numCalificaciones numeric\n");
        sb.append("@attribute recomendar {SI,NO}\n\n");
        sb.append("@data\n");

        // Datos estáticos de entrenamiento (75 instancias)
        agregarDatosEstaticos(sb);

        // Datos dinámicos desde la BD
        agregarDatosDB(sb);

        return sb.toString();
    }

    // Normaliza el nombre de la categoría al valor ARFF válido
    public String normalizarCategoria(String categoria) {
        if (categoria == null) return null;
        String upper = categoria.trim().toUpperCase()
                .replace(" ", "_")
                .replace("CIENCIA FICCION", "CIENCIA_FICCION")
                .replace("CIENCIAFICCION",  "CIENCIA_FICCION");
        return CATEGORIAS_VALIDAS.contains(upper) ? upper : null;
    }

    // Devuelve SI si el género está entre los favoritos del usuario
    public String generoEsFavorito(String generosFavoritos, String categoriaNormalizada) {
        if (generosFavoritos == null || generosFavoritos.isBlank()) return "NO";
        boolean favorito = Arrays.stream(generosFavoritos.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .anyMatch(g -> g.equals(categoriaNormalizada));
        return favorito ? "SI" : "NO";
    }

    // ── Privado ──────────────────────────────────────────────────────────────

    private void agregarDatosDB(StringBuilder sb) {
        try {
            List<Calificacion> todas = calificacionRepo.findAll();
            List<Usuario>      usuarios   = usuarioRepo.findAll();
            List<Pelicula>     peliculas  = peliculaRepo.findAll();

            Map<String, Usuario>  mapUsuarios  = usuarios.stream()
                    .collect(Collectors.toMap(Usuario::getId, u -> u));
            Map<String, Pelicula> mapPeliculas = peliculas.stream()
                    .collect(Collectors.toMap(Pelicula::getId, p -> p));

            // Agrupar calificaciones por usuario para contar numCalificaciones por género
            Map<String, List<Calificacion>> porUsuario = todas.stream()
                    .collect(Collectors.groupingBy(Calificacion::getUsuarioId));

            for (Calificacion c : todas) {
                if (c.getPuntuacion() == null || c.getUsuarioId() == null || c.getPeliculaId() == null) continue;

                Usuario  u = mapUsuarios.get(c.getUsuarioId());
                Pelicula p = mapPeliculas.get(c.getPeliculaId());
                if (u == null || p == null) continue;

                String cat = normalizarCategoria(p.getCategoria());
                if (cat == null) continue;

                double edad       = u.getEdad() != null ? u.getEdad() : 25;
                double puntuacion = c.getPuntuacion();
                String esFav      = generoEsFavorito(u.getGenerosFavoritos(), cat);

                // Contar cuántas películas de ese género ha calificado el usuario
                long numCal = porUsuario.getOrDefault(u.getId(), List.of()).stream()
                        .filter(cx -> {
                            Pelicula px = mapPeliculas.get(cx.getPeliculaId());
                            return px != null && cat.equals(normalizarCategoria(px.getCategoria()));
                        })
                        .count();

                String recomendar = (puntuacion >= 4.0) ? "SI" : "NO";

                sb.append(edad).append(",")
                  .append(cat).append(",")
                  .append(puntuacion).append(",")
                  .append(esFav).append(",")
                  .append(Math.min(numCal, 10)).append(",")
                  .append(recomendar).append("\n");
            }
        } catch (Exception e) {
            // Si la BD no está disponible se usa sólo datos estáticos
            System.err.println("[DatasetServicio] No se pudo cargar datos de BD: " + e.getMessage());
        }
    }

    private void agregarDatosEstaticos(StringBuilder sb) {
        // 75 instancias: 15 por cada categoría, distribución balanceada SI/NO
        String[] filas = {
            // ACCION
            "20,ACCION,4.5,SI,5,SI",
            "25,ACCION,4.0,SI,3,SI",
            "30,ACCION,5.0,NO,4,SI",
            "35,ACCION,4.5,SI,2,SI",
            "40,ACCION,4.0,NO,3,SI",
            "22,ACCION,2.0,NO,0,NO",
            "28,ACCION,1.5,NO,1,NO",
            "33,ACCION,2.5,SI,0,NO",
            "45,ACCION,3.0,NO,0,NO",
            "50,ACCION,1.0,NO,0,NO",
            "26,ACCION,3.5,SI,1,SI",
            "38,ACCION,3.0,SI,2,SI",
            "19,ACCION,2.5,NO,1,NO",
            "55,ACCION,4.0,NO,5,SI",
            "42,ACCION,3.5,NO,1,NO",
            // ROMANCE
            "18,ROMANCE,4.5,SI,4,SI",
            "23,ROMANCE,5.0,SI,5,SI",
            "27,ROMANCE,4.0,NO,3,SI",
            "32,ROMANCE,4.5,SI,2,SI",
            "38,ROMANCE,4.0,SI,4,SI",
            "21,ROMANCE,2.0,NO,0,NO",
            "29,ROMANCE,1.5,NO,0,NO",
            "34,ROMANCE,2.5,NO,1,NO",
            "41,ROMANCE,1.0,NO,0,NO",
            "48,ROMANCE,2.0,SI,0,NO",
            "24,ROMANCE,3.5,SI,1,SI",
            "36,ROMANCE,3.0,SI,3,SI",
            "19,ROMANCE,2.0,NO,0,NO",
            "52,ROMANCE,4.0,NO,5,SI",
            "44,ROMANCE,3.5,NO,1,NO",
            // TERROR
            "20,TERROR,4.5,SI,5,SI",
            "26,TERROR,4.0,NO,3,SI",
            "31,TERROR,5.0,SI,4,SI",
            "37,TERROR,4.5,SI,2,SI",
            "43,TERROR,4.0,NO,3,SI",
            "22,TERROR,2.0,NO,0,NO",
            "28,TERROR,1.5,SI,0,NO",
            "33,TERROR,2.5,NO,1,NO",
            "46,TERROR,1.0,NO,0,NO",
            "51,TERROR,2.0,NO,0,NO",
            "25,TERROR,3.5,SI,1,SI",
            "39,TERROR,3.0,SI,2,SI",
            "18,TERROR,2.5,NO,0,NO",
            "54,TERROR,4.0,SI,5,SI",
            "42,TERROR,3.5,NO,1,NO",
            // COMEDIA
            "22,COMEDIA,4.5,SI,5,SI",
            "27,COMEDIA,4.0,SI,3,SI",
            "32,COMEDIA,5.0,NO,4,SI",
            "36,COMEDIA,4.5,SI,2,SI",
            "41,COMEDIA,4.0,NO,3,SI",
            "19,COMEDIA,2.0,NO,0,NO",
            "25,COMEDIA,1.5,NO,1,NO",
            "30,COMEDIA,2.5,NO,0,NO",
            "44,COMEDIA,1.0,NO,0,NO",
            "49,COMEDIA,2.0,NO,0,NO",
            "23,COMEDIA,3.5,SI,1,SI",
            "37,COMEDIA,3.0,SI,2,SI",
            "18,COMEDIA,2.5,NO,0,NO",
            "56,COMEDIA,4.0,SI,5,SI",
            "43,COMEDIA,3.5,NO,1,NO",
            // CIENCIA_FICCION
            "21,CIENCIA_FICCION,4.5,SI,5,SI",
            "26,CIENCIA_FICCION,4.0,SI,3,SI",
            "31,CIENCIA_FICCION,5.0,NO,4,SI",
            "35,CIENCIA_FICCION,4.5,SI,2,SI",
            "40,CIENCIA_FICCION,4.0,NO,3,SI",
            "23,CIENCIA_FICCION,2.0,NO,0,NO",
            "29,CIENCIA_FICCION,1.5,NO,1,NO",
            "34,CIENCIA_FICCION,2.5,SI,0,NO",
            "47,CIENCIA_FICCION,1.0,NO,0,NO",
            "53,CIENCIA_FICCION,2.0,NO,0,NO",
            "27,CIENCIA_FICCION,3.5,SI,1,SI",
            "38,CIENCIA_FICCION,3.0,SI,2,SI",
            "20,CIENCIA_FICCION,2.5,NO,0,NO",
            "57,CIENCIA_FICCION,4.0,NO,5,SI",
            "44,CIENCIA_FICCION,3.5,NO,1,NO"
        };
        for (String fila : filas) {
            sb.append(fila).append("\n");
        }
    }
}
