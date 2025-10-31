package com.example.bitpacking.cli;
import java.util.Arrays;

import com.example.bitpacking.api.BitPacker;
import com.example.bitpacking.core.*;
import com.example.bitpacking.factory.BitPackerFactory;


public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: <mode: split|nosplit|overflow> <comma-separated-int-list>");
            System.exit(1);
        }

        final String mode = args[0];
        final int[] input = parseCsvInts(args[1]);

        // ---- Factory: un seul paramètre, retourne la bonne implémentation
        final BitPacker packer;
        try {
            packer = BitPackerFactory.create(mode);
        } catch (Exception e) {
            System.err.println("Factory error: " + e.getMessage());
            System.exit(2);
            return; // pour le compilateur
        }

        // ---- Compression
        long t0 = System.nanoTime();
        BitPackedArray compressed = packer.compress(input);
        long t1 = System.nanoTime();

        // ---- Décompression
        int[] restored = packer.decompress(compressed);
        long t2 = System.nanoTime();

        // ---- Affichages & vérifs
        System.out.println("Mode        : " + mode);
        System.out.println("Input size  : " + input.length);
        System.out.println("Input (head): " + head(input, 16));
        System.out.println();
        System.out.println("Compressed:");
        System.out.println("  n = " + compressed.n() + " | k = " + compressed.k() + " | words.length = " + compressed.words().length);
        if (packer instanceof OverflowBitPacker) {
            System.out.println("  overflowStartBit = " + compressed.overflowStartBit() + " | kOverflow = " + compressed.kOverflow() + " | overflowCount = " + compressed.overflowCount() + " | kIndex = " + compressed.kIndex());
        }
        System.out.println("  words (int): " + Arrays.toString(compressed.words()));
        System.out.println("  words (bin):");
        for (int i = 0; i < compressed.words().length; i++) {
            System.out.printf("    w[%d] = %s%n", i, to32Bits(compressed.words()[i]));
        }

        boolean ok = Arrays.equals(input, restored);
        System.out.println();
        System.out.println("Decompressed (head): " + head(restored, 16));
        System.out.println("OK? " + ok);

        System.out.printf("Timing      : compress=%.3f ms | decompress=%.3f ms%n",
                (t1 - t0)/1e6, (t2 - t1)/1e6);

       // ---- Tests get() sur tous les indices ----
        if (input.length > 0) {
            System.out.println();
            System.out.println("get() full check:");
            boolean allOk = true;

            for (int idx = 0; idx < input.length; idx++) {
                int got = packer.get(compressed, idx);
                boolean ok2 = (got == input[idx]);
                if (!ok2) allOk = false;
                System.out.printf("  get(%d) = %d %s%n", idx, got, ok2? "✓" : "✗");
            }

        System.out.println();
        System.out.println(allOk ? "✅ Tous les indices sont corrects."
                             : "❌ Des divergences ont été détectées.");
        }


        // ---- Optionnel: afficher l’input en binaire 32 bits (si tu veux)
        // System.out.println("\nInput (bin 32):");
        // for (int i = 0; i < input.length; i++) {
        //     System.out.printf("  in[%d] = %s%n", i, to32Bits(input[i]));
        // }
    }

    private static int[] parseCsvInts(String csv) {
        String[] parts = csv.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim();
            if (s.isEmpty()) throw new IllegalArgumentException("Empty value at position " + i);
            // On reste en int positifs (contrat du projet)
            int v = Integer.parseUnsignedInt(s);
            if (v < 0) throw new IllegalArgumentException("Negative not allowed: " + v);
            arr[i] = v;
        }
        return arr;
    }

    private static String head(int[] a, int limit) {
        if (a == null) return "null";
        int n = Math.min(a.length, limit);
        int[] h = Arrays.copyOfRange(a, 0, n);
        String suffix = (a.length > limit) ? " … (+" + (a.length - limit) + ")" : "";
        return Arrays.toString(h) + suffix;
    }

    private static String to32Bits(int x) {
        String s = Integer.toBinaryString(x);
        if (s.length() < 32) {
            StringBuilder sb = new StringBuilder(32);
            for (int i = s.length(); i < 32; i++) sb.append('0');
            sb.append(s);
            s = sb.toString();
        }
        // groupement visuel /8 pour lecture
        return s.substring(0,8) + "_" + s.substring(8,16) + "_" + s.substring(16,24) + "_" + s.substring(24,32);
    }

}