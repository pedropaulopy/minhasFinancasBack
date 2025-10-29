package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import java.util.Optional;

public interface UsuarioService {
  Usuario autenticar(String email, String senha) throws RegraNegocioException;

  Usuario salvarUsuario(Usuario usuario) throws RegraNegocioException;

  void validarEmail(String email) throws RegraNegocioException;

  Optional<Usuario> obterPorId(Long id) throws RegraNegocioException;

  Usuario obterIdUsuarioPorEmail(String email) throws RegraNegocioException;
}
