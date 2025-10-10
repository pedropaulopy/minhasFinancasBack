package com.pedropaulo.minhasFinancas.service.impl;

import com.pedropaulo.minhasFinancas.service.UsuarioService;
import model.entity.Usuario;
import model.repository.UsuarioRepository;

public class UsuarioServiceImpl implements UsuarioService {

    private UsuarioRepository repository;

    public UsuarioServiceImpl(UsuarioRepository repository) {
        super();
        this.repository = repository;
    }

    @Override
    public Usuario autenticar(String email, String senha) {
        return null;
    }

    @Override
    public Usuario salvarUsuario(Usuario usuario) {
        return null;
    }

    @Override
    public void validarEmail(String email) {

    }
}
