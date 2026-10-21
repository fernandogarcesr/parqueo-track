package mx.itson.devops.parqueo;

import java.util.HashSet;
import java.util.Set;

/**
 * Controla quién está dentro del estacionamiento y cuántos lugares quedan.
 *
 * Regla acordada con el área de operaciones:
 *   - La capacidad es fija (se define al crear). Debe ser mayor que 0.
 *   - ingresar(placa): placa null o en blanco -> IllegalArgumentException.
 *     Si ya está dentro -> IllegalStateException. Si NO hay lugar (está lleno) -> IllegalStateException.
 *     La placa se guarda en mayúsculas y sin espacios al inicio/final.
 *   - salir(placa): si no está dentro -> IllegalStateException.
 *   - Con capacidad 3 caben EXACTAMENTE 3 autos; el cuarto debe ser rechazado.
 */
public class Estacionamiento {

    private final int capacidad;
    private final Set<String> adentro = new HashSet<>();

    public Estacionamiento(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("capacidad debe ser mayor que 0");
        }
        this.capacidad = capacidad;
    }

    public void ingresar(String placa) {
        String p = normalizar(placa);
        if (adentro.contains(p)) {
            throw new IllegalStateException("la placa ya esta dentro: " + p);
        }
        if (adentro.size() > capacidad) {
            throw new IllegalStateException("estacionamiento lleno");
        }
        adentro.add(p);
    }

    public void salir(String placa) {
        String p = normalizar(placa);
        if (!adentro.remove(p)) {
            throw new IllegalStateException("la placa no esta dentro: " + p);
        }
    }

    public boolean estaDentro(String placa) {
        String p = normalizar(placa);
        if (adentro.contains(p) == true) {
            return true;
        } else {
            return false;
        }
    }

    public int ocupados() {
        return adentro.size();
    }

    public int disponibles() {
        return capacidad - adentro.size();
    }

    private String normalizar(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("placa requerida");
        }
        return placa.trim().toUpperCase();
    }
}
