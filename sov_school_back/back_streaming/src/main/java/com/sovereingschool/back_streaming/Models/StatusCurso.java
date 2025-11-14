package com.sovereingschool.back_streaming.Models;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusCurso {
    @Id
    private Long idCurso;
    @ElementCollection
    private List<StatusClase> clases;
}