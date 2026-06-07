package com.streamflix.repositorio;

import com.streamflix.modelo.Pelicula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PeliculaRepositorio extends JpaRepository<Pelicula, String> {

    @Query("SELECT p FROM Pelicula p WHERE p.id NOT IN " +
           "(SELECT c.peliculaId FROM Calificacion c WHERE c.usuarioId = :usuarioId)")
    List<Pelicula> findPeliculasNoVistasPorUsuario(@Param("usuarioId") String usuarioId);
}
