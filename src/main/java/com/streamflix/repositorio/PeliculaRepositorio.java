package com.streamflix.repositorio;

import com.streamflix.modelo.Pelicula;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeliculaRepositorio
        extends JpaRepository<Pelicula, String> {
}
