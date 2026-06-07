package com.streamflix.repositorio;

import com.streamflix.modelo.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CalificacionRepositorio extends JpaRepository<Calificacion, Long> {

    List<Calificacion> findByUsuarioId(String usuarioId);

    @Query("SELECT AVG(c.puntuacion) FROM Calificacion c WHERE c.usuarioId = :usuarioId")
    Double promedioGlobalUsuario(@Param("usuarioId") String usuarioId);
}
