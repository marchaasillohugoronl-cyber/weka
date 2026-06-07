package com.streamflix.modelo;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "peliculas")
@Data
public class Pelicula {

    @Id
    private String id;

    private String titulo;
    private String descripcion;
    private String categoria;
    private Integer anio;
    private Integer duracionMinutos;
}
