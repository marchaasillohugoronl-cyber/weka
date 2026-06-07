package com.streamflix.repositorio;

import com.streamflix.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, String> {
}
