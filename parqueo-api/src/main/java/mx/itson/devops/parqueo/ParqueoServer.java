package mx.itson.devops.parqueo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Servidor HTTP mínimo (viene con el JDK, no necesita ninguna librería).
 *
 *   GET /health                                  -> {"status":"UP"}
 *   GET /api/cobro?minutos=75&pase=ITSON20       -> {"minutos":75,"pase":"ITSON20","total":14}
 *
 * Si algo sale mal responde 400 con {"error":"..."}.
 */
public class ParqueoServer {

    private final HttpServer server;
    private final CobroService cobro = new CobroService();

    public ParqueoServer(int puerto) throws IOException {
        server = HttpServer.create(new InetSocketAddress(puerto), 0);
        server.createContext("/health", this::health);
        server.createContext("/api/cobro", this::cobrar);
    }

    public void iniciar() {
        server.start();
    }

    public void detener() {
        server.stop(0);
    }

    public int puerto() {
        return server.getAddress().getPort();
    }

    private void health(HttpExchange ex) throws IOException {
        responder(ex, 200, "{\"status\":\"UP\"}");
    }

    private void cobrar(HttpExchange ex) throws IOException {
        try {
            Map<String, String> q = parsearQuery(ex.getRequestURI().getRawQuery());
            if (!q.containsKey("minutos")) {
                throw new IllegalArgumentException("falta el parametro minutos");
            }
            int minutos = Integer.parseInt(q.get("minutos"));
            String pase = q.get("pase");
            int total = cobro.cobrar(minutos, pase);
            responder(ex, 200, "{\"minutos\":" + minutos + ",\"pase\":\"" + (pase == null ? "" : pase)
                    + "\",\"total\":" + total + "}");
        } catch (IllegalArgumentException e) {
            responder(ex, 400, "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    static Map<String, String> parsearQuery(String raw) {
        Map<String, String> mapa = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return mapa;
        }
        for (String par : raw.split("&")) {
            String[] kv = par.split("=", 2);
            String clave = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String valor = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            mapa.put(clave, valor);
        }
        return mapa;
    }

    private void responder(HttpExchange ex, int codigo, String json) throws IOException {
        byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(codigo, cuerpo.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(cuerpo);
        }
    }
}
