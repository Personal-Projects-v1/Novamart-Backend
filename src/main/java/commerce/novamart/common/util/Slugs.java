package commerce.novamart.common.util;

import java.util.Locale;

public final class Slugs {

    private Slugs() {
    }

    public static String of(String input) {
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
