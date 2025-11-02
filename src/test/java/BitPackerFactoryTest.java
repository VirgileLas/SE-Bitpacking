import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.example.bitpacking.api.*;
import com.example.bitpacking.core.*;
import com.example.bitpacking.factory.*;

public class BitPackerFactoryTest {

    @Test
    void testCreateNoSplit() {
        BitPacker packer = BitPackerFactory.create("nosplit");
        assertTrue(packer instanceof NoSplitBitPacker, "Factory should create a NoSplitBitPacker");
    }

    @Test
    void testCreateSplit() {
        BitPacker packer = BitPackerFactory.create("split");
        assertTrue(packer instanceof SplitBitPacker, "Factory should create a SplitBitPacker");
    }

    @Test
    void testCreateOverflow() {
        BitPacker packer = BitPackerFactory.create("overflow");
        assertTrue(packer instanceof OverflowBitPacker, "Factory should create an OverflowBitPacker");
    }

    @Test
    void testFactoryIsCaseInsensitive() {
        BitPacker packer = BitPackerFactory.create("NoSplit");
        assertTrue(packer instanceof NoSplitBitPacker, "Factory should ignore case in type name");
    }

    @Test
    void testFactoryInvalidNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> BitPackerFactory.create("invalid"));
    }

    @Test
    void testFactoryProducesWorkingObjects() {
        // nosplit
        BitPacker noSplit = BitPackerFactory.create("nosplit");
        int[] arr = {1, 2, 3, 4, 5};
        BitPackedArray comp = noSplit.compress(arr);
        assertArrayEquals(arr, noSplit.decompress(comp));

        // split
        BitPacker split = BitPackerFactory.create("split");
        comp = split.compress(arr);
        assertArrayEquals(arr, split.decompress(comp));

        // overflow
        BitPacker overflow = BitPackerFactory.create("overflow");
        int[] arr2 = {1, 2, 3, 999, 4, 5, 1024};
        comp = overflow.compress(arr2);
        assertArrayEquals(arr2, overflow.decompress(comp));
    }
}
