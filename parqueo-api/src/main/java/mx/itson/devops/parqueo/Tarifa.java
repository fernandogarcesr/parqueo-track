package mx.itson.devops.parqueo;

/**
 * Calcula cuánto se cobra por el tiempo que un auto estuvo en el estacionamiento.
 *
 * Regla acordada con el área de operaciones (en pesos, enteros):
 *   0 a 15 minutos (INCLUYENDO el 15)  -> $0   (tiempo de gracia)
 *   16 a 60 minutos                    -> $10
 *   más de 60 minutos                  -> $10 + $8 por cada hora o FRACCIÓN adicional
 *   tope por visita                    -> $80 como máximo
 *   minutos negativos                  -> IllegalArgumentException
 *
 * Ejemplos de la regla: 15 min = $0 | 16 min = $10 | 61 min = $18 | 121 min = $26 | 600 min = $80
 */
public class Tarifa {

    static final int TARIFA_BASE = 10;
    static final int TARIFA_HORA_ADICIONAL = 8;
    static final int TOPE_POR_VISITA = 80;

    public int calcular(int minutos) {
        if (minutos < 0) {
            throw new IllegalArgumentException("minutos negativos: " + minutos);
        }
        if (minutos < 15) {
            return 0;
        }
        if (minutos <= 60) {
            return TARIFA_BASE;
        }
        int horasAdicionales = (minutos - 60) / 60;
        int total = TARIFA_BASE + horasAdicionales * TARIFA_HORA_ADICIONAL;
        return Math.min(total, TOPE_POR_VISITA);
    }
}
