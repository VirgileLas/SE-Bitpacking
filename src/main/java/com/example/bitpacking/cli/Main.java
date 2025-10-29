package com.example.bitpacking.cli;
import java.util.Arrays;

import com.example.bitpacking.api.BitPacker;
import com.example.bitpacking.core.*;

public class Main {
    public static void main(String[] args) {
        int[] original = {3, 7, 15, 2, 31, 0, 1, 8, 5, 12};
        Arrays.stream(original).forEach(x -> System.out.println(String.format("%32s", Integer.toBinaryString(x)).replace(' ', '0')));
        System.out.println("Original array:");
        printArray(original);

        BitPacker packer = new OverflowBitPacker();

        // Compression
        BitPackedArray compressed = packer.compress(original);
        System.out.println("\nCompressed words:");
        for (int word : compressed.words()) {
            System.out.printf("%s ", Long.toBinaryString(word | 0x100000000L).substring(1));
        }
        System.out.println("\nk = " + compressed.k() + ", n = " + compressed.n());

        // Décompression
        int[] decompressed = packer.decompress(compressed);
        System.out.println("\nDecompressed array:");
        printArray(decompressed);

        // Vérification get(i)
        boolean ok = true;
        for (int i = 0; i < original.length; i++) {
            int val = packer.get(compressed, i);
            if (val != original[i]) {
                System.out.printf("❌ get(%d) = %d, expected %d\n", i, val, original[i]);
                ok = false;
            }
        }

        System.out.println(ok ? "\n✅ get(i) is correct for all indices." : "\n❌ Error in get(i).");
        System.out.println((equal(original, decompressed)) ? "✅ Decompression is correct." : "❌ Decompression mismatch.");
    }

    private static void printArray(int[] array) {
        for (int x : array) System.out.print(x + " ");
        System.out.println();
    }

    private static boolean equal(int[] a, int[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }
}
