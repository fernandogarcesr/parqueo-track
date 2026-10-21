package mx.itson.devops.avisos;

/**
 * Arma el texto del recibo que se le muestra a la persona al salir.
 *
 * Regla acordada con el área comercial:
 *   recibo(placa, total) -> "Placa ABC123: total a pagar $14"
 *   La placa se muestra en MAYÚSCULAS y sin espacios al inicio/final.
 *   placa null o en blanco -> IllegalArgumentException
 *   total negativo         -> IllegalArgumentException
 *   total 0                -> "Placa ABC123: sin cargo"
 */
public class AvisoService {

    public String recibo(String placa, int total) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("placa requerida");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total negativo: " + total);
        }
        String p = placa.trim().toUpperCase();
        if (total == 0) {
            return "Placa " + p + ": sin cargo";
        }
        return "Placa " + p + ": total a pagar $" + total;
    }
}
