package edu.wisc.ece.pinpoint.utils;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nullable;

public class FormatUtils {
    public static String humanReadablePinnies(Long pinniesCount) {
        String suffix;
        double value = pinniesCount;

        if (pinniesCount >= 1_000_000_000) {
            value /= 1_000_000_000;
            suffix = "B";
        } else if (pinniesCount >= 1_000_000) {
            value /= 1_000_000;
            suffix = "M";
        } else if (pinniesCount >= 1_000) {
            value /= 1_000;
            suffix = "K";
        } else {
            return pinniesCount.toString();
        }

        return String.format(Locale.US, "%.2f%s", value, suffix);
    }

    @Nullable
    public static String formattedPinLocation(@Nullable String broadLocationName,
                                              @Nullable String nearbyLocationName) {
        return nearbyLocationName != null ?
                String.format("%s in %s", nearbyLocationName, broadLocationName) :
                broadLocationName;
    }

    public static String formattedActivityLocation(@Nullable String broadLocationName,
                                                   @Nullable String nearbyLocationName) {
        return nearbyLocationName != null ?
                String.format("near %s in %s", nearbyLocationName, broadLocationName) :
                broadLocationName != null ? String.format("in %s", broadLocationName) : "";
    }

    public static String formattedDateTime(Date timestamp) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(timestamp);
    }

    public static String formattedDate(Date timestamp) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp);
    }

    public static String formattedTime(Date timestamp) {
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(timestamp);
    }
}
