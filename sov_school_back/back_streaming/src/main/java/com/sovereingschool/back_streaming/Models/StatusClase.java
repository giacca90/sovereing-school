package com.sovereingschool.back_streaming.Models;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusClase {

    private Long idClase;

    private boolean completed;

    /**
     * El "Mapa del tesoro": Cuántos fragmentos (.ts) tiene el video en total.
     * Se debe setear al cargar la clase por primera vez.
     */
    private int totalSegments;

    /**
     * El "Camino recorrido": Guardamos los índices de los fragmentos vistos.
     * Usamos un Set para que, aunque el usuario repita un fragmento,
     * no se guarde dos veces ni afecte al porcentaje.
     */
    @Builder.Default
    private Set<Integer> progress = new HashSet<>();

    /**
     * Método de conveniencia para obtener el progreso real en porcentaje.
     * Se puede llamar desde el Getter de Jackson para enviarlo al Front.
     */
    public double getProgressPercentage() {
        if (totalSegments <= 0)
            return 0.0;
        double calculation = (double) progress.size() / totalSegments * 100.0;
        // Retornamos el valor limitado a 100 por si acaso
        return Math.min(calculation, 100.0);
    }
}