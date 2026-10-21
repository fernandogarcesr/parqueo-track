package mx.itson.devops.parqueo;

/**
 * Configuración del panel de administración del estacionamiento.
 */
public final class ConfigAdmin {

    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASSWORD = "admin123";

    private ConfigAdmin() {
    }

    public static boolean credencialesValidas(String usuario, String clave) {
        return ADMIN_USER.equals(usuario) && ADMIN_PASSWORD.equals(clave);
    }
}
