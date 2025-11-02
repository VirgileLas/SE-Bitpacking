import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Random;

import com.example.bitpacking.api.*;
import com.example.bitpacking.core.*;

public class RandomizedPropertyTests {

    private final Random rnd = new Random(42);

    private void checkAllPackers(int[] a) {
        BitPacker[] ps = {
            new NoSplitBitPacker(),
            new SplitBitPacker(),
            new OverflowBitPacker()
        };
        for (BitPacker p : ps) {
            BitPackedArray c = p.compress(a);
            int[] out = p.decompress(c);
            assertArrayEquals(a, out, p.getClass().getSimpleName());
            for (int i=0;i<a.length;i++) {
                assertEquals(out[i], p.get(c, i), "get!=decompress @"+i+" "+p.getClass().getSimpleName());
            }
        }
    }

    @Test
    void random_small_values_many_cases() {
        for (int t=0;t<50;t++) {
            int n = 50 + rnd.nextInt(150);
            int[] a = new int[n];
            int cap = 1 << (1 + rnd.nextInt(12)); // max 4096
            for (int i=0;i<n;i++) a[i] = rnd.nextInt(cap);
            checkAllPackers(a);
        }
    }

    @Test
    void nonRegression_zero_phantom_pattern() {
        int[] a = {0, 1, 2, 3, 0, 4096, 0, 5, 6, 0, 7, 0, 8, 0};
        checkAllPackers(a);
    }
}
