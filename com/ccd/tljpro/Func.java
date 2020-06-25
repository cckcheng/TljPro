package com.ccd.tljpro;

import java.util.ArrayList;
import java.util.List;

/**
 * misc functions
 *
 * @author ccheng
 */
public class Func {

    public static String trimmedString(Object obj) {
        if (obj == null) return "";
        return obj.toString().trim();
    }

    public static int parseInteger(Object obj) {
        if (obj == null) return -1;
        try {
            return (int) Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean parseBoolean(Object obj) {
        if (obj == null) return false;
        String r = obj.toString();
        return r.equalsIgnoreCase("yes") || r.equalsIgnoreCase("true");
    }

    public static List<String> toStringList(String str, char delim) {
        List<String> lst = new ArrayList<>();
        if (str == null || str.trim().isEmpty()) return lst;
        do {
            int idx = str.indexOf(delim);
            if (idx < 0) {
                lst.add(str);
                break;
            }
            lst.add(str.substring(0, idx));
            str = str.substring(idx + 1);
        } while (true);
        return lst;
    }
}
