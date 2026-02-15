package com.sovereingschool.back_base.Interfaces;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;

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

    public String getNombreUsuario(Long idUsuario) throws Exception;

    public List<String> getFotosUsuario(Long idUsuario);

    public RoleEnum getRollUsuario(Long idUsuario) throws Exception;

    public Plan getPlanUsuario(Long idUsuario) throws Exception;

    public List<Curso> getCursosUsuario(Long idUsuario) throws Exception;

    public Usuario updateUsuario(Usuario usuario) throws InternalServerException, Exception;

    public Integer changePlanUsuario(Usuario usuario) throws Exception;

    public Integer changeCursosUsuario(CursosUsuario cursosUsuario) throws RepositoryException, Exception;

    public String deleteUsuario(Long id) throws RepositoryException, InternalComunicationException, Exception;

    public List<Usuario> getProfes() throws Exception;

    public boolean sendConfirmationEmail(NewUsuario newUsuario) throws MessagingException, InternalServerException,
            AccessDeniedException;

    public List<Usuario> getAllUsuarios() throws RepositoryException, Exception;
}
