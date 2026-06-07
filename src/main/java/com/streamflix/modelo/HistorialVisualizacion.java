package com.streamflix.modelo;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_visualizacion")
@Data
public class HistorialVisualizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuarioId;
    private String peliculaId;
    private Integer porcentajeVisto;
    private LocalDateTime fechaVisualizacion;
}
