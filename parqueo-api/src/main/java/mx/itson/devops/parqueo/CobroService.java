package mx.itson.devops.parqueo;

/**
 * Junta las dos reglas: primero la tarifa por tiempo y DESPUÉS el descuento del pase.
 *
 * Regla acordada con el área comercial:
 *   total = tarifa(minutos) * (100 - descuento) / 100
 *   Es aritmética ENTERA: se trunca (no se redondea). Ejemplo: $18 con 20 % -> 18*80/100 = 14.
 *   Ejemplos: 30 min sin pase = $10 | 30 min con ITSON20 = $8 | 61 min con ITSON20 = $14
 */
public class CobroService {

    private final Tarifa tarifa = new Tarifa();
    private final PaseDescuento pase = new PaseDescuento();

    public int cobrar(int minutos, String codigoPase) {
        int total = tarifa.calcular(minutos);
        int descuento = pase.porcentaje(codigoPase);
        return total - (total / 100 * descuento);
    }
}
