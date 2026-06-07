package com.streamflix.modelo;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {

    @Id
    private String id;

    private String nombre;
    private String correo;
    private Integer edad;
    private String pais;
    private String generosFavoritos;
}
