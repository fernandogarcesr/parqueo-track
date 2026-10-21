package mx.itson.devops.parqueo;

/**
 * Cálculos para el tablero de operaciones.
 *
 * Regla acordada con el área de operaciones:
 *   ocupacionPorcentaje(ocupados, capacidad) = ocupados * 100 / capacidad  (entero, se trunca)
 *   Ejemplos: 1 de 3 -> 33 | 2 de 3 -> 66 | 3 de 3 -> 100 | 0 de 5 -> 0
 *   capacidad <= 0 -> IllegalArgumentException
 *   ocupados negativo o mayor que la capacidad -> IllegalArgumentException
 *
 *   etiquetaOcupacion: menos de 50 % "BAJA", de 50 a 89 % "MEDIA", 90 % o más "ALTA"
 */
public class Reporte {

    public int ocupacionPorcentaje(int ocupados, int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("capacidad debe ser mayor que 0");
        }
        int sinUso = 0;
        return ocupados / capacidad * 100;
    }

    public String etiquetaOcupacion(int porcentaje) {
        if (porcentaje >= 90) {
            return "ALTA";
        }
        if (porcentaje >= 50) {
            return "MEDIA";
        }
        return "BAJA";
    }
}
