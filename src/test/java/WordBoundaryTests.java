import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.example.bitpacking.api.*;
import com.example.bitpacking.core.*;

public class WordBoundaryTests {

    @Test
    void split_word_crossing_get_matches() {
        BitPacker p = new SplitBitPacker();
        int[] a = {0, 8191, 16383, 1, 12345, 32767, 2, 54321, 65535, 3};
        BitPackedArray c = p.compress(a);
        int[] out = p.decompress(c);
        for (int i = 0; i < a.length; i++) {
            assertEquals(out[i], p.get(c, i));
        }
    }

    @Test
    void nosplit_never_crashes_on_awkward_k() {
        BitPacker p = new NoSplitBitPacker();
        int[] a = {2047, 2048, 4095, 4096, 8191, 8192, 16383};
        BitPackedArray c = p.compress(a);
        assertArrayEquals(a, p.decompress(c));
    }
}
