import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Arrays;

import com.example.bitpacking.api.*;
import com.example.bitpacking.core.*;

public class ConformanceTests {

    private static int bitWidth(int x) { return (x == 0) ? 1 : 32 - Integer.numberOfLeadingZeros(x); }

    @Test
    void nosplit_roundTrip_and_k_matches_max() {
        BitPacker p = new NoSplitBitPacker();
        int[] a = {0, 1, 2, 3, 7, 8, 15, 16, 255, 256, 4095};
        int max = Arrays.stream(a).max().orElse(0);
        BitPackedArray c = p.compress(a);
        assertArrayEquals(a, p.decompress(c));
        assertEquals(bitWidth(max), c.k(), "k doit refléter le max (spec bit packing).");
    }

    @Test
    void split_roundTrip_on_boundary_case_k12_len6() {
        BitPacker p = new SplitBitPacker();
        int[] a = {0xABC, 0x001, 0x123, 0xF00, 0x00F, 0x888}; // cas pédagogique
        BitPackedArray c = p.compress(a);
        assertArrayEquals(a, p.decompress(c));
    }
}
