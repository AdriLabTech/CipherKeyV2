package dev.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import dev.persistence.AppPrefs;

/** Tema claro/oscuro con FlatLaf. La elección se guarda en {@link AppPrefs}. */
public final class ThemeManager {
    private ThemeManager() {}

    /** Instala el Look & Feel según la preferencia guardada. Llamar antes de crear ventanas. */
    public static void install() {
        FlatLaf.registerCustomDefaultsSource("dev.view.themes");
        setLaf(AppPrefs.isDark());
    }

    public static void toggle() {
        boolean dark = !isDark();
        AppPrefs.setDark(dark);
        FlatAnimatedLafChange.showSnapshot();
        setLaf(dark);
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }

    public static boolean isDark() {
        return FlatLaf.isLafDark();
    }

    private static void setLaf(boolean dark) {
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
    }
}
