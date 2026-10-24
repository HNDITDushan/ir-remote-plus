package com.universal.irremote.ir;

import java.util.ArrayList;
import java.util.List;

/** A carrier frequency plus alternating on/off durations in microseconds. */
public final class IrPattern {
    public final int frequency;
    public final int[] pattern;

    public IrPattern(int frequency, int[] pattern) {
        this.frequency = frequency;
        this.pattern = pattern;
    }

    /** Builds a pattern, merging consecutive marks or spaces. Must start with a mark. */
    static final class Builder {
        private final int frequency;
        private final List<Integer> items = new ArrayList<>();
        private boolean lastIsMark = false;

        Builder(int frequency) {
            this.frequency = frequency;
        }

        Builder mark(int us) {
            add(us, true);
            return this;
        }

        Builder space(int us) {
            add(us, false);
            return this;
        }

        private void add(int us, boolean isMark) {
            if (us <= 0) return;
            if (items.isEmpty()) {
                if (!isMark) return; // patterns always begin with the LED on
                items.add(us);
            } else if (isMark == lastIsMark) {
                items.set(items.size() - 1, items.get(items.size() - 1) + us);
            } else {
                items.add(us);
            }
            lastIsMark = isMark;
        }

        /** Sends bits of {@code data}, either LSB-first or MSB-first. */
        Builder bits(long data, int count, boolean msbFirst,
                     int oneMark, int oneSpace, int zeroMark, int zeroSpace) {
            for (int i = 0; i < count; i++) {
                int shift = msbFirst ? count - 1 - i : i;
                boolean one = ((data >>> shift) & 1L) == 1L;
                mark(one ? oneMark : zeroMark);
                space(one ? oneSpace : zeroSpace);
            }
            return this;
        }

        IrPattern build() {
            int[] out = new int[items.size()];
            for (int i = 0; i < out.length; i++) out[i] = items.get(i);
            return new IrPattern(frequency, out);
        }
    }
}
