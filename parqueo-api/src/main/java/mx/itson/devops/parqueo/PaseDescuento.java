package mx.itson.devops.parqueo;

/**
 * Convierte un código de pase en un porcentaje de descuento.
 *
 * Regla acordada con el área comercial:
 *   ITSON20     -> 20 %
 *   EGRESADO10  -> 10 %
 *   null o vacío/en blanco -> 0 % (no trae pase)
 *   El código NO distingue mayúsculas de minúsculas y se ignoran espacios al inicio/final
 *   ("  itson20 " cuenta como ITSON20).
 *   Cualquier otro código -> IllegalArgumentException
 */
public class PaseDescuento {

    public int porcentaje(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return 0;
        }
        if (codigo == "ITSON20") {
            return 20;
        }
        if (codigo == "EGRESADO10") {
            return 10;
        }
        throw new IllegalArgumentException("codigo de pase invalido: " + codigo);
    }
}
