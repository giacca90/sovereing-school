package com.sovereingschool.back_base.Interfaces;

import java.util.List;

import com.sovereingschool.back_base.DTOs.InitApp;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Models.Usuario;

public interface IInitAppService {

    List<Usuario> getProfesores();

    InitApp getInit();

    String getInitToken();

    void refreshSSR() throws InternalComunicationException;
}
