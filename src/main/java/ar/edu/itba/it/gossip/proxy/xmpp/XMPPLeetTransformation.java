package ar.edu.itba.it.gossip.proxy.xmpp;

import java.util.function.Function;

public class XMPPLeetTransformation implements Function<String, String> {
    private static final String ESCAPED_LT = "&lt;";

    @Override
    public String apply(String originalXML) {
        String transformedXML = "";

        for (int i = 0; i < originalXML.length(); i++) {
            char originalChar = originalXML.charAt(i);
            transformedXML += transform(originalChar);
        }

        return transformedXML;
    }

    private String transform(char originalChar) {
        switch (originalChar) {
        case 'a':
            return "4";
        case 'e':
            return "3";
        case 'i':
            return "1";
        case 'o':
            return "0";
        case 'c':
            return ESCAPED_LT;
        default:
            return String.valueOf(originalChar);
        }
    }
}
