package playground.test.revo.util;

public class StringUtils {
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean notNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }
}
