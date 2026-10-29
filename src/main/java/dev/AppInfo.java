package dev;

import java.io.InputStream;
import java.util.Properties;

/** Datos de la aplicación leídos de {@code app.properties} (filtrado por Maven). */
public final class AppInfo {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppInfo.class.getResourceAsStream("/app.properties")) {
            if (in != null) PROPS.load(in);
        } catch (Exception ignored) {
            // sin fichero se usan los valores por defecto
        }
    }

    private AppInfo() {}

    public static String version() {
        return PROPS.getProperty("version", "dev");
    }
}
