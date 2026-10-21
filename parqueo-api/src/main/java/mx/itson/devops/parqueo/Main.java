package mx.itson.devops.parqueo;

import java.io.IOException;

/**
 * Punto de entrada. El puerto sale de la variable de entorno PORT (por omisión 8080).
 * Así la misma imagen funciona en tu laptop, en Docker y en AWS sin cambiar el código.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        String valor = System.getenv("PORT");
        int puerto = valor == null || valor.isBlank() ? 8080 : Integer.parseInt(valor.trim());
        ParqueoServer servidor = new ParqueoServer(puerto);
        servidor.iniciar();
        System.out.println("ParqueoTrack escuchando en el puerto " + servidor.puerto());
    }
}
