package edu.wisc.ece.pinpoint.utils;

public class FormatUtils {
    public static String humanReadablePinnies(Long pinniesCount) {
        String suffix = "";
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

        return String.format("%.2f%s", value, suffix);
    }
}
