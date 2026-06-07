package com.streamflix.servicio;

import com.streamflix.repositorio.PeliculaRepositorio;
import org.springframework.stereotype.Service;

@Service
public class PeliculaServicio {

    private final PeliculaRepositorio repositorio;

    public PeliculaServicio(
            PeliculaRepositorio repositorio) {

        this.repositorio = repositorio;
    }
}
