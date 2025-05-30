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

    public Long createCurso(Curso new_curso);

    public Curso getCurso(Long id_curso);

    public String getNombreCurso(Long id_curso);

    public List<Usuario> getProfesoresCurso(Long id_curso);

    public Date getFechaCreacionCurso(Long id_curso);

    public List<Clase> getClasesDelCurso(Long id_curso);

    public List<Plan> getPlanesDelCurso(Long id_curso);

    public BigDecimal getPrecioCurso(Long id_curso);

    public Curso updateCurso(Curso curso);

    public Boolean deleteCurso(Long id_curso);

    public void deleteClase(Clase clase);

    public String subeVideo(MultipartFile file);
}
