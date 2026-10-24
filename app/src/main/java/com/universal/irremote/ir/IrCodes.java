package com.universal.irremote.ir;

import java.util.Locale;

/**
 * Text format used to store button codes:
 * <pre>
 *   NEC:00:45            NEC, address 0x00, command 0x45 (address > FF = extended NEC)
 *   SYM:D81              Symphony 12-bit fan code
 *   COOLIX:B27BE0        Raw 24-bit Coolix / Midea code
 *   PRONTO:0000 006D ... Pronto hex from an online IR database
 *   RAW:38000:9000,4500,560,...   frequency then on/off durations in microseconds
 * </pre>
 */
public final class IrCodes {
    private IrCodes() {}

    public static final String HELP =
            "Code formats:\n"
                    + "NEC:00:45  (address:command, hex)\n"
                    + "SYM:D81  (12-bit fan code, hex)\n"
                    + "COOLIX:B27BE0  (24-bit, hex)\n"
                    + "PRONTO:0000 006D 0022 ...\n"
                    + "RAW:38000:9000,4500,560,560,...";

    public static IrPattern parse(String code) {
        String c = code.trim();
        int colon = c.indexOf(':');
        if (colon < 0) throw new IllegalArgumentException("Missing protocol prefix, e.g. NEC:00:45");
        String proto = c.substring(0, colon).trim().toUpperCase(Locale.ROOT);
        String body = c.substring(colon + 1).trim();
        switch (proto) {
            case "NEC": {
                String[] p = body.split(":");
                if (p.length != 2) throw new IllegalArgumentException("NEC needs address:command");
                return Protocols.nec(Integer.parseInt(p[0].trim(), 16), Integer.parseInt(p[1].trim(), 16));
            }
            case "SYM":
                return Protocols.symphony(Integer.parseInt(body, 16));
            case "COOLIX":
                return Protocols.coolix(Integer.parseInt(body, 16));
            case "PRONTO":
                return Protocols.pronto(body);
            case "RAW": {
                int c2 = body.indexOf(':');
                if (c2 < 0) throw new IllegalArgumentException("RAW needs frequency:durations");
                int freq = Integer.parseInt(body.substring(0, c2).trim());
                String[] d = body.substring(c2 + 1).split("[,\\s]+");
                int[] pattern = new int[d.length];
                for (int i = 0; i < d.length; i++) pattern[i] = Math.abs(Integer.parseInt(d[i].trim()));
                return new IrPattern(freq, pattern);
            }
            default:
                throw new IllegalArgumentException("Unknown protocol: " + proto);
        }
    }

    public static String nec(int address, int command) {
        return String.format(Locale.ROOT, address > 0xFF ? "NEC:%04X:%02X" : "NEC:%02X:%02X", address, command);
    }

    public static String symphony(int code) {
        return String.format(Locale.ROOT, "SYM:%03X", code);
    }
}
