package com.sovereingschool.back_base.Interfaces;

import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.DTOs.ChangePassword;
import com.sovereingschool.back_common.Models.Login;
import com.sovereingschool.back_common.Models.Usuario;

public interface ILoginService {

    public Long compruebaCorreo(String correo);

    public String createNuevoLogin(Login login);

    public String getCorreoLogin(Long idUsuario);

    public String getPasswordLogin(Long idUsuario);

    public String changeCorreoLogin(Login login);

    public Integer changePasswordLogin(ChangePassword changepassword);

    public String deleteLogin(Long idUsuario);

    public AuthResponse loginUser(Long id, String password);

    public AuthResponse refreshAccessToken(Long id);

    public Usuario loginWithToken(String token);
}
