import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.example.bitpacking.api.*;
import com.example.bitpacking.core.*;
import com.example.bitpacking.factory.*;

public class RobustnessTests {

    @Test
    void get_outOfBounds_throws() {
        BitPacker p = new OverflowBitPacker();
        int[] a = {1,2,3};
        BitPackedArray c = p.compress(a);
        assertThrows(IndexOutOfBoundsException.class, () -> p.get(c, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> p.get(c, 3));
    }

    @Test
    void compress_nullArray_throws() {
        BitPacker p = new NoSplitBitPacker();
        assertThrows(NullPointerException.class, () -> p.compress(null));
    }

    @Test
    void factory_invalid_name_throws() {
        assertThrows(IllegalArgumentException.class, () -> BitPackerFactory.create("wut"));
    }
}
