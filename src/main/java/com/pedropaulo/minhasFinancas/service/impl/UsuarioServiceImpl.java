package com.pedropaulo.minhasFinancas.service.impl;

import com.pedropaulo.minhasFinancas.exception.AutenticacaoException;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.service.UsuarioService;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private UsuarioRepository repository;
    private PasswordEncoder encoder;

    @Autowired
    public UsuarioServiceImpl(UsuarioRepository repository, PasswordEncoder encoder) {
        super();
        this.repository = repository;
        this.encoder = encoder;
    }

    @Override
    public Usuario autenticar(String email, String senha) throws RegraNegocioException {
        Optional<Usuario> usuario = repository.findByEmail(email);
        if(!usuario.isPresent()){
            throw new AutenticacaoException("Usuário não encontrado para o email informado.");
        }

        boolean samePassword = encoder.matches(senha, usuario.get().getSenha());

        if(!samePassword){
            throw new AutenticacaoException("Senha inválida.");
        }
        return usuario.get();

    }

    public void criptografarSenha(Usuario usuario){
        String senha = usuario.getSenha();
        String senhaCripto = encoder.encode(senha);
        usuario.setSenha(senhaCripto);
    }

    @Override
    @Transactional
    public Usuario salvarUsuario(Usuario usuario) throws RegraNegocioException {
        validarEmail(usuario.getEmail());
        criptografarSenha(usuario);
        return repository.save(usuario);
    }

    @Override
    public void validarEmail(String email) throws RegraNegocioException {
       boolean exists = repository.existsByEmail(email);
        if(exists){
            throw new RegraNegocioException("Um usuário já foi cadastrado com este email.");
        }
    }

    @Override
    public Optional<Usuario> obterPorId(Long id) throws RegraNegocioException {
        return repository.findById(id);
    }
}
