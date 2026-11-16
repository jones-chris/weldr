package ai.weldr.spreadsheet_welder.codegen;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public final class CellValueConverter {

    private CellValueConverter() {}

    public static String asString(Object val) {
        return val == null ? null : val.toString();
    }

    public static double asDouble(Object val) {
        return switch (val) {
            case null -> 0.0; // Or throw if required field
            case Number n -> n.doubleValue();
            case String s when !s.isBlank() -> Double.parseDouble(s.trim());
            default -> throw new IllegalArgumentException("Cannot convert value to double: " + val);
        };
    }

    public static int asInt(Object val) {
        return switch (val) {
            case null -> 0;
            case Number n -> n.intValue();
            case String s when !s.isBlank() -> Integer.parseInt(s.trim());
            default -> throw new IllegalArgumentException("Cannot convert value to int: " + val);
        };
    }

    public static boolean asBoolean(Object val) {
        return switch (val) {
            case null -> false;
            case Boolean b -> b;
            case String s -> Boolean.parseBoolean(s.trim());
            default -> throw new IllegalArgumentException("Cannot convert value to boolean: " + val);
        };
    }

    public static Date asDate(Object val) {
        switch (val) {
            case null -> {
                return null;
            }
            case Date d -> {
                return d;
            }
            case LocalDate ld -> {
                return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
            }
            case Number n -> {
                // Handles raw Excel numeric dates if POI didn't already convert them
                return org.apache.poi.ss.usermodel.DateUtil.getJavaDate(n.doubleValue());
                // Handles raw Excel numeric dates if POI didn't already convert them
            }
            case String s when !s.isBlank() -> {
                LocalDate ld = LocalDate.parse(s.trim()); // ISO-8601 (yyyy-MM-dd)

                return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant()); // ISO-8601 (yyyy-MM-dd)
            }
            default -> {
            }
        }

        throw new IllegalArgumentException("Cannot convert value to Date: " + val);
    }

}
