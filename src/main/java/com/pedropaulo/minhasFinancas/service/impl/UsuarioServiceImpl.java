package com.pedropaulo.minhasFinancas.service.impl;

import com.pedropaulo.minhasFinancas.exception.AutenticacaoException;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.repository.UsuarioRepository;
import com.pedropaulo.minhasFinancas.service.UsuarioService;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

  private final UsuarioRepository repository;
  private final PasswordEncoder encoder;

  @Autowired
  public UsuarioServiceImpl(UsuarioRepository repository, PasswordEncoder encoder) {
    super();
    this.repository = repository;
    this.encoder = encoder;
  }

  @Override
  public Usuario autenticar(String email, String senha) throws RegraNegocioException {
    Optional<Usuario> usuario = repository.findByEmail(email);
    if (!usuario.isPresent()) {
      throw new AutenticacaoException("Usuário não encontrado para o email informado.");
    }

    boolean samePassword = encoder.matches(senha, usuario.get().getSenha());

    if (!samePassword) {
      throw new AutenticacaoException("Senha inválida.");
    }
    return usuario.get();
  }

  public void criptografarSenha(Usuario usuario) {
    String senha = usuario.getSenha();
    String senhaCripto = encoder.encode(senha);
    usuario.setSenha(senhaCripto);
  }

  @Override
  @Transactional
  public Usuario salvarUsuario(Usuario usuario) throws RegraNegocioException {
    validarEmail(usuario.getEmail());
    criptografarSenha(usuario);
    usuario.setDataCadastro(LocalDate.now());
    return repository.save(usuario);
  }

  @Override
  public void validarEmail(String email) throws RegraNegocioException {
    boolean exists = repository.existsByEmail(email);
    if (exists) {
      throw new RegraNegocioException("Um usuário já foi cadastrado com este email.");
    }
  }

  @Override
  public Optional<Usuario> obterPorId(Long id) throws RegraNegocioException {
      if (!repository.findById(id).isPresent()) {
          throw new RegraNegocioException(
          "Usuário não encontrado para o ID informado.");
      }
      return repository.findById(id);
  }

  public Long obterIdPorEmail(String email) throws RegraNegocioException {
      Usuario usuario = repository.findByEmail(email).orElseThrow(() -> new RegraNegocioException("Usuário não encontrado para o email informado"));
      return usuario.getId();
  }
}
