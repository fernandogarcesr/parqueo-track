package mx.itson.devops.parqueo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de humo (smoke): lo mínimo para saber que "algo" funciona.
 */
class ParqueoSmokeTest {

    @Test
    void sinTiempoNoSeCobra() {
        assertEquals(0, new Tarifa().calcular(0));
    }

    @Test
    void mediaHoraCuestaLaTarifaBase() {
        assertEquals(10, new Tarifa().calcular(30));
    }

    @Test
    void minutosNegativosSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> new Tarifa().calcular(-1));
    }

    @Test
    void sinPaseNoHayDescuento() {
        assertEquals(0, new PaseDescuento().porcentaje(null));
    }

    @Test
    void elEstacionamientoRecibeAutos() {
        Estacionamiento e = new Estacionamiento(3);
        e.ingresar("ABC123");
        assertTrue(e.estaDentro("abc123"));
        assertEquals(2, e.disponibles());
    }
}
