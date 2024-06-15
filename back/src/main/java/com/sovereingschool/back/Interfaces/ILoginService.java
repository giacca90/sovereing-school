package com.sovereingschool.back.Interfaces;

import com.sovereingschool.back.Models.ChangePassword;
import com.sovereingschool.back.Models.Login;

public interface ILoginService {
    public String createNuevoLogin(Login login);

    public String getCorreoLogin(Long id_usuario);

    public String getPasswordLogin(Long id_usuario);

    public String changeCorreoLogin(Login login);

    public String changePasswordLogin(ChangePassword changepassword);

    public String deleteLogin(Long id_usuario);
}
