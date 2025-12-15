package com.sovereingschool.back_base.Interfaces;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sovereingschool.back_base.DTOs.ClaseDTO;
import com.sovereingschool.back_base.DTOs.CursoDTO;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.NotFoundException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;
import com.sovereingschool.back_common.Exceptions.ServiceException;
import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.Usuario;

public interface ICursoService {
    public List<Curso> getAll();

    public Long createCurso(Curso newCurso) throws RepositoryException;

    public Curso getCurso(Long idCurso) throws NotFoundException;

    public String getNombreCurso(Long idCurso) throws NotFoundException;

    public List<Usuario> getProfesoresCurso(Long idCurso) throws NotFoundException;

    public Date getFechaCreacionCurso(Long idCurso) throws NotFoundException;

    public List<Clase> getClasesDelCurso(Long idCurso) throws NotFoundException;

    public List<Plan> getPlanesDelCurso(Long idCurso) throws NotFoundException;

    public BigDecimal getPrecioCurso(Long idCurso) throws NotFoundException;

    public Curso updateCurso(Curso curso)
            throws NotFoundException, InternalServerException, InternalComunicationException,
            RepositoryException;

    public Boolean deleteCurso(Long idCurso) throws ServiceException;

    public void deleteClase(Clase clase) throws ServiceException;

    public String subeVideo(MultipartFile file) throws InternalServerException;

    public Curso cursoDTOToCurso(CursoDTO cursoDTO) throws NotFoundException;

    public Clase claseDTOToClase(ClaseDTO claseDTO);

}
