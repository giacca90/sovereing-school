package com.sovereingschool.back_base.Interfaces;

import java.util.List;

import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.DTOs.CursosUsuario;
import com.sovereingschool.back_common.DTOs.NewUsuario;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;

import jakarta.mail.MessagingException;

public interface IUsuarioService {
    public AuthResponse createUsuario(NewUsuario newUsuario) throws RepositoryException, InternalComunicationException;

    public Usuario getUsuario(Long idUsuario);

    public String getNombreUsuario(Long idUsuario);

    public List<String> getFotosUsuario(Long idUsuario);

    public RoleEnum getRollUsuario(Long idUsuario);

    public Plan getPlanUsuario(Long idUsuario);

    public List<Curso> getCursosUsuario(Long idUsuario);

    public Usuario updateUsuario(Usuario usuario) throws InternalServerException;

    public Integer changePlanUsuario(Usuario usuario);

    public Integer changeCursosUsuario(CursosUsuario cursosUsuario);

    public String deleteUsuario(Long id) throws RepositoryException;

    public List<Usuario> getProfes();

    public boolean sendConfirmationEmail(NewUsuario newUsuario) throws MessagingException, InternalServerException;

    public List<Usuario> getAllUsuarios() throws RepositoryException;
}
