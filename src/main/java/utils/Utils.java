package utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class Utils {

    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen; UTF-8 is pretty standard
            throw new IllegalArgumentException("Invalid encoding", e);
        }
    }

    public static String urlDecode(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen; UTF-8 is pretty standard
            throw new IllegalArgumentException("Invalid encoding", e);
        }
    }

}
