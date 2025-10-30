package com.example.bitpacking.factory;

import com.example.bitpacking.api.BitPacker;
import com.example.bitpacking.core.NoSplitBitPacker;
import com.example.bitpacking.core.SplitBitPacker;


public final class BitPackerFactory {
    private BitPackerFactory() {}

    // Une seule méthode, un seul paramètre (String mode)
    public static BitPacker create(String mode) {
        if (mode == null) throw new IllegalArgumentException("mode is null");
        String m = mode.trim().toLowerCase();

        switch (m) {
            case "split":
                return new SplitBitPacker();
            case "nosplit":
                return new NoSplitBitPacker();
            case "overflow":
                // return new OverflowBitPacker();
                throw new UnsupportedOperationException("Overflow not implemented yet");
            default:
                throw new IllegalArgumentException("Unknown mode '"+mode+"'. Use: split|nosplit|overflow");
        }
    }
}
