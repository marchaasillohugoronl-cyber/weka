package com.streamflix.repositorio;

import com.streamflix.modelo.HistorialVisualizacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialVisualizacionRepositorio extends JpaRepository<HistorialVisualizacion, Long> {
}
