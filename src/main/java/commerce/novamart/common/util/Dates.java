package commerce.novamart.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class Dates {

    private Dates() {
    }

    /** ISO-8601 instant string (with Z) so browsers parse it unambiguously. */
    public static String iso(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant().toString();
    }
}
