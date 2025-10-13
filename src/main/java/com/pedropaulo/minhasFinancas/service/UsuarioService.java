package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;

public interface UsuarioService {
    Usuario autenticar(String email, String senha) throws RegraNegocioException;
    Usuario salvarUsuario(Usuario usuario) throws RegraNegocioException;
    void validarEmail(String email) throws RegraNegocioException;
}