import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.example.bitpacking.api.*;
import com.example.bitpacking.factory.*;
import com.example.bitpacking.core.*;

public class IntegrationFactoryModesTest {

    @Test
    void all_modes_handle_same_array() {
        String[] modes = {"nosplit","split","overflow"};
        int[] a = {0,1,2,3, 1024, 4,5, 2048, 0, 7, 65535};
        for (String m : modes) {
            BitPacker p = BitPackerFactory.create(m);
            BitPackedArray c = p.compress(a);
            assertArrayEquals(a, p.decompress(c), "Mode="+m);
            for (int i=0;i<a.length;i++) assertEquals(a[i], p.get(c,i), "get mismatch @"+i+" mode="+m);
        }
    }
}
