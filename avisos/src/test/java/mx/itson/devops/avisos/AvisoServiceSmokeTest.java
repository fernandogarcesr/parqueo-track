package mx.itson.devops.avisos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Fercho
 */
public class AvisoServiceSmokeTest {

    @Test
    void sinCargoCuandoElTotalEsCero() {
        // Arrange: preparo el servicio
        AvisoService servicio = new AvisoService();
        // Act: pido el recibo de un total de 0
        String recibo = servicio.recibo("abc123", 0);
        // Assert: debe decir que no hay cargo
        assertEquals("Placa ABC123: sin cargo", recibo);
    }
}
