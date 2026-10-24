package com.universal.irremote.ir;

/**
 * Encoders for the IR protocols most often used by fans, TVs and air conditioners.
 * Timings follow the IRremoteESP8266 reference implementation.
 */
public final class Protocols {
    private Protocols() {}

    // ---------------------------------------------------------------- NEC
    // Used by most TVs, set-top boxes, LED lights and many fans.

    /** Standard NEC with an 8-bit address, or extended NEC if address > 0xFF. */
    public static IrPattern nec(int address, int command) {
        long addr = address > 0xFF ? (address & 0xFFFF) : ((address & 0xFF) | ((~address & 0xFF) << 8));
        long data = addr | ((long) (command & 0xFF) << 16) | ((long) (~command & 0xFF) << 24);
        return new IrPattern.Builder(38000)
                .mark(9000).space(4500)
                .bits(data, 32, false, 560, 1690, 560, 560)
                .mark(560).space(40000)
                // One NEC repeat frame, like holding the button briefly.
                .mark(9000).space(2250).mark(560)
                .build();
    }

    // ----------------------------------------------------------- Symphony
    // 12-bit protocol used by many Chinese pedestal, wall and tower fans.

    public static IrPattern symphony(int code) {
        IrPattern.Builder b = new IrPattern.Builder(38000);
        for (int i = 0; i < 3; i++) {
            b.bits(code & 0xFFF, 12, true, 1250, 400, 400, 1250);
            b.space(6600);
        }
        return b.build();
    }

    // -------------------------------------------------------------- Gree
    // Gree and the many unbranded/rebadged split ACs built on Gree boards.

    public static final int MODE_AUTO = 0, MODE_COOL = 1, MODE_DRY = 2, MODE_FAN = 3, MODE_HEAT = 4;
    public static final int FAN_AUTO = 0, FAN_LOW = 1, FAN_MED = 2, FAN_HIGH = 3;

    public static byte[] greeState(boolean power, int mode, int temp, int fan, boolean swing) {
        byte[] s = new byte[8];
        int t = Math.max(16, Math.min(30, temp)) - 16;
        s[0] = (byte) ((mode & 7) | (power ? 0x08 : 0) | ((fan & 3) << 4) | (swing ? 0x40 : 0));
        s[1] = (byte) t;
        s[2] = 0x20;              // display light on
        s[3] = 0x50;              // fixed model bits
        s[4] = (byte) (swing ? 0x01 : 0x00);
        s[5] = 0x20;
        s[6] = 0x00;
        s[7] = 0x00;
        int sum = 10;
        for (int i = 0; i < 4; i++) sum += s[i] & 0x0F;
        for (int i = 4; i < 7; i++) sum += (s[i] & 0xF0) >> 4;
        s[7] = (byte) (((sum << 4) & 0xF0) | (s[7] & 0x0F));
        return s;
    }

    public static IrPattern gree(byte[] s) {
        IrPattern.Builder b = new IrPattern.Builder(38000).mark(9000).space(4500);
        for (int i = 0; i < 4; i++) b.bits(s[i] & 0xFF, 8, false, 620, 1600, 620, 540);
        b.bits(0b010, 3, false, 620, 1600, 620, 540);
        b.mark(620).space(19980);
        for (int i = 4; i < 8; i++) b.bits(s[i] & 0xFF, 8, false, 620, 1600, 620, 540);
        b.mark(620);
        return b.build();
    }

    // ------------------------------------------------------------ Coolix
    // Midea "R51" remotes, also used by many Midea-built OEM brands.

    private static final int[] COOLIX_TEMP = {
            0b0000, 0b0001, 0b0011, 0b0010, 0b0110, 0b0111, 0b0101,
            0b0100, 0b1100, 0b1101, 0b1001, 0b1000, 0b1010, 0b1011};
    public static final int COOLIX_OFF = 0xB27BE0;
    public static final int COOLIX_SWING = 0xB26BE0;

    public static int coolixState(int mode, int temp, int fan) {
        int t = Math.max(17, Math.min(30, temp)) - 17;
        int tempCode = COOLIX_TEMP[t];
        int modeCode;
        switch (mode) {
            case MODE_COOL: modeCode = 0b00; break;
            case MODE_DRY:  modeCode = 0b01; break;
            case MODE_HEAT: modeCode = 0b11; break;
            case MODE_FAN:  modeCode = 0b01; tempCode = 0b1110; break;
            default:        modeCode = 0b10; break; // auto
        }
        int fanCode;
        if (mode == MODE_AUTO || mode == MODE_DRY) {
            fanCode = 0b000;
        } else {
            switch (fan) {
                case FAN_LOW:  fanCode = 0b100; break;
                case FAN_MED:  fanCode = 0b010; break;
                case FAN_HIGH: fanCode = 0b001; break;
                default:       fanCode = mode == MODE_FAN ? 0b001 : 0b101; break;
            }
        }
        return 0xB20000 | ((fanCode << 5 | 0x1F) << 8) | (tempCode << 4) | (modeCode << 2);
    }

    public static IrPattern coolix(int code24) {
        IrPattern.Builder b = new IrPattern.Builder(38000);
        for (int r = 0; r < 2; r++) {
            b.mark(4692).space(4416);
            for (int shift = 16; shift >= 0; shift -= 8) {
                int v = (code24 >> shift) & 0xFF;
                b.bits(v, 8, true, 552, 1656, 552, 552);
                b.bits(~v & 0xFF, 8, true, 552, 1656, 552, 552);
            }
            b.mark(552).space(5244);
        }
        return b.build();
    }

    // ----------------------------------------------------------- Electra
    // AUX-made units and their YKR-x/xxxE remotes (AUX, Electra, Centek, DB fan coils...).

    public static byte[] electraState(boolean power, int mode, int temp, int fan, boolean swing) {
        byte[] s = new byte[13];
        int t = Math.max(16, Math.min(32, temp)) - 8;
        int swingBits = swing ? 0b000 : 0b111;
        int fanBits;
        switch (fan) {
            case FAN_LOW:  fanBits = 0b011; break;
            case FAN_MED:  fanBits = 0b010; break;
            case FAN_HIGH: fanBits = 0b001; break;
            default:       fanBits = 0b101; break;
        }
        int modeBits;
        switch (mode) {
            case MODE_COOL: modeBits = 0b001; break;
            case MODE_DRY:  modeBits = 0b010; break;
            case MODE_HEAT: modeBits = 0b100; break;
            case MODE_FAN:  modeBits = 0b110; break;
            default:        modeBits = 0b000; break;
        }
        s[0] = (byte) 0xC3;
        s[1] = (byte) (swingBits | (t << 3));
        s[2] = (byte) (swingBits << 5);   // horizontal swing
        s[4] = (byte) (fanBits << 5);
        s[6] = (byte) (modeBits << 5);
        s[9] = (byte) (power ? 0x20 : 0);
        s[11] = 0x08;                     // light toggle: no change
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += s[i] & 0xFF;
        s[12] = (byte) sum;
        return s;
    }

    public static IrPattern electra(byte[] s) {
        IrPattern.Builder b = new IrPattern.Builder(38000).mark(9166).space(4470);
        for (byte v : s) b.bits(v & 0xFF, 8, false, 646, 1647, 646, 547);
        return b.mark(646).build();
    }

    // ------------------------------------------------------------ Pronto

    /** Parses a Pronto hex code ("0000 006D 0022 0002 ..."), as found in online IR databases. */
    public static IrPattern pronto(String text) {
        String[] parts = text.trim().split("\\s+");
        int[] w = new int[parts.length];
        for (int i = 0; i < parts.length; i++) w[i] = Integer.parseInt(parts[i], 16);
        if (w.length < 6 || w[0] != 0) throw new IllegalArgumentException("Only learned Pronto (0000) codes are supported");
        int freq = (int) Math.round(1_000_000.0 / (w[1] * 0.241246));
        int once = w[2], repeat = w[3];
        int start = 4, pairs = once > 0 ? once : repeat;
        double period = 1_000_000.0 / freq;
        IrPattern.Builder b = new IrPattern.Builder(freq);
        for (int i = 0; i < pairs * 2 && start + i < w.length; i++) {
            int us = (int) Math.round(w[start + i] * period);
            if (i % 2 == 0) b.mark(us); else b.space(us);
        }
        if (once > 0 && repeat > 0) {
            int rs = 4 + once * 2;
            for (int i = 0; i < repeat * 2 && rs + i < w.length; i++) {
                int us = (int) Math.round(w[rs + i] * period);
                if (i % 2 == 0) b.mark(us); else b.space(us);
            }
        }
        return b.build();
    }
}
