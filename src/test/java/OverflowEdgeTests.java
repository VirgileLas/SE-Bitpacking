import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.example.bitpacking.api.*;
import com.example.bitpacking.core.*;

public class OverflowEdgeTests {

    @Test
    void overflow_sparse_big_values_roundTrip_and_get() {
        BitPacker p = new OverflowBitPacker();
        int[] a = {1,2,3, 1024, 4,5, 2048};
        BitPackedArray c = p.compress(a);
        int[] out = p.decompress(c);
        assertArrayEquals(a, out);

        for (int i = 0; i < a.length; i++) {
            assertEquals(out[i], p.get(c, i));
        }
        assertTrue(c.overflowCount() >= 1, "Devrait contenir une zone overflow non vide.");
    }

    @Test
    void overflow_all_small_then_big_tail() {
        BitPacker p = new OverflowBitPacker();
        int[] a = new int[64];
        for (int i=0;i<60;i++) a[i] = i & 7;
        a[60] = 4095; a[61] = 4096; a[62] = 65535; a[63] = 1<<20;
        BitPackedArray c = p.compress(a);
        assertArrayEquals(a, p.decompress(c));
    }
}
