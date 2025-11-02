package com.sovereingschool.back_streaming.Models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class StatusClase {
    @Id
    private Long idClase;
    private boolean completed;
    private int progress; // Representa el progreso en la clase, por ejemplo en segundos o porcentaje
}