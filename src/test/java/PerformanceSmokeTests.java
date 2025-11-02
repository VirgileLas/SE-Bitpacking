import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Arrays;

import com.example.bitpacking.api.*;
import com.example.bitpacking.core.*;

public class PerformanceSmokeTests {

    @Test
    void overflow_smoke_large_n() {
        BitPacker p = new OverflowBitPacker();
        int n = 100_000; // smoke rapide
        int[] a = new int[n];
        for (int i=0;i<n;i++) a[i] = (i%20==0) ? (1<<20) : (i & 0xFF);
        BitPackedArray c = p.compress(a);
        int[] out = p.decompress(c);
        assertEquals(n, c.n());
        assertArrayEquals(a, out);
        double bitsPerVal = (c.words().length * 32.0) / n;
        assertTrue(bitsPerVal < 32.0, "La compression devrait être utile sur ce dataset.");
    }

    @Test
    void split_smoke_uniform_small() {
        BitPacker p = new SplitBitPacker();
        int n = 300_000;
        int[] a = new int[n];
        Arrays.fill(a, 7);
        BitPackedArray c = p.compress(a);
        assertArrayEquals(a, p.decompress(c));
        assertTrue(c.words().length > 0);
    }
}
