package com.streamflix.modelo;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "calificaciones")
@Data
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuarioId;
    private String peliculaId;
    private Integer puntuacion;
}
