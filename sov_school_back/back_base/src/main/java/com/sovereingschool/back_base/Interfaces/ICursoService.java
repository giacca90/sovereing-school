package com.sovereingschool.back_base.Interfaces;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sovereingschool.back_common.Models.Clase;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.Usuario;

public interface ICursoService {
    public List<Curso> getAll();

    public Long createCurso(Curso newCurso);

    public Curso getCurso(Long idCurso);

    public String getNombreCurso(Long idCurso);

    public List<Usuario> getProfesoresCurso(Long idCurso);

    public Date getFechaCreacionCurso(Long idCurso);

    public List<Clase> getClasesDelCurso(Long idCurso);

    public List<Plan> getPlanesDelCurso(Long idCurso);

    public BigDecimal getPrecioCurso(Long idCurso);

    public Curso updateCurso(Curso curso);

    public Boolean deleteCurso(Long idCurso);

    public void deleteClase(Clase clase);

    public String subeVideo(MultipartFile file);
}
