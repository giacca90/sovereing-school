package com.sovereingschool.back_streaming.Models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class StatusClase {
    @Id
    private Long idClase;
    private boolean completed;
    private int progress; // Representa el progreso en la clase, por ejemplo en segundos o porcentaje
}