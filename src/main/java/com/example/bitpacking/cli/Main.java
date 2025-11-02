package com.example.bitpacking.cli;
import java.util.Arrays;
import java.util.function.IntFunction;


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
        

        // ---- Factory: un seul paramètre, retourne la bonne implémentation
        final BitPacker packer;
        try {
            packer = BitPackerFactory.create(mode);
        } catch (Exception e) {
            System.err.println("Factory error: " + e.getMessage());
            System.exit(2);
            return; 
        }

        
        
        //BENCHMARK A RETIRER
        
          if ("--bench".equalsIgnoreCase(args[1])) {
        runBenchmark(packer);   // ta méthode benchmark
        System.exit(0);
        return; // pour le compilateur
    }
        

        final int[] input = parseCsvInts(args[1]);




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
                (t1 - t0)/1e6, (t2 - t1)/1e6); // divise par 1 puissance -6 pour avoir le temps en ms 

  
        // Test get pour un échantillion 
        if (input.length > 0) {
            int samples = Math.min(input.length, 32);
            boolean allOk = true;
            System.out.println();
            System.out.println("get() sampled check:");
            for (int i = 0; i < samples; i++) {
                int idx = (int) ((long) i * (input.length - 1) / Math.max(1, samples - 1));
                long t3 = System.nanoTime();
                int got = packer.get(compressed, idx);
                long t4 = System.nanoTime();             
                boolean ok_get = (got == input[idx]);
                if (!ok_get) allOk = false;
                  System.out.printf("  get(%d) = %d (%s) | %.1f ns%n",idx, got, ok_get ? "ok" : "not ok", (t4 - t3/1e6));
            }
            System.out.println(allOk ? "échantillon OK." : "divergences détectées.");
        }

    }

    private static int[] parseCsvInts(String csv) { // Verifie que le tableau est bien conforme au règle imposé entier positif et pas de valeur null
        String[] parts = csv.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim();
            if (s.isEmpty()) throw new IllegalArgumentException("Valeur vide " + i);
            // On reste en int positifs (contrat du projet)
            int v = Integer.parseInt(s);
            if (v < 0) throw new IllegalArgumentException("Pas de négatid autorisé " + v);
            arr[i] = v;
        }
        return arr;
    }

    private static String head(int[] a, int limit) { // Affiche un tableau donné de sa première valeur à la limite fixé ou sa fin si length < limit 
        if (a == null) return "null";
        int n = Math.min(a.length, limit);
        int[] h = Arrays.copyOfRange(a, 0, n);
        String suffix = (a.length > limit) ? " … (+" + (a.length - limit) + ")" : "";
        return Arrays.toString(h) + suffix;
    }

    private static String to32Bits(int x) { // afficher le flux de bit découpé en mot de 32 eux même découpé en segment de 8 affichage standar des bit 
        String s = Integer.toBinaryString(x);
        if (s.length() < 32) {
            StringBuilder sb = new StringBuilder(32);
            for (int i = s.length(); i < 32; i++) sb.append('0');
            sb.append(s);
            s = sb.toString();
        }
        return s.substring(0,8) + "_" + s.substring(8,16) + "_" + s.substring(16,24) + "_" + s.substring(24,32);
    }
    private static void runBenchmark(BitPacker packer) {
    System.out.println("\n=== BENCHMARK MODE ===\n");

    java.util.Random rnd = new java.util.Random(42);

    // 6 scénarios de tests
    final String[] labels = {
        "petit aléatoire (max=255)",
        "mix aléatoire (5% gros)",
        "quasi pas compressible (valeurs 30b)",
        "tout petit sauf 1 énorme",
        "majorité petits, 10% overflow",
        "100% énorme (aucun gain possible)"
    };
        @SuppressWarnings("unchecked")
        final IntFunction<int[]>[] datasets = new IntFunction[] {
        // 1. tout petit
        (int n) -> {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(256);
            return a;
        },

        // 2. aléatoire avec 5% très gros
        (int n) -> {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                a[i] = (rnd.nextDouble() < 0.05) ? (1 << 20) + rnd.nextInt(100000) : rnd.nextInt(4096);
            }
            return a;
        },

        // 3. valeurs de 30 bits → presque aucun gain
        (int n) -> {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = (1 << 29) + rnd.nextInt(1 << 2);
            return a;
        },

        // 4. tous petits sauf 1 très grand
        (int n) -> {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = 7;
            a[n / 2] = (1 << 24);
            return a;
        },

        // 5. 10% overflow
        (int n) -> {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                a[i] = (i % 10 == 0) ? (1 << 20) + i : rnd.nextInt(1024);
            }
            return a;
        },

        // 6. tout énorme
        (int n) -> {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = (1 << 28) + rnd.nextInt(1 << 3);
            return a;
        }
    };

    final int[] sizes = {1_000, 100_000, 1_000_000};
    for (int s = 0; s < sizes.length; s++) {
    int n = sizes[s];
    System.out.printf("%n--- Taille tableau : %d éléments ---\n", n);
    for (int i = 0; i < datasets.length; i++) {
        int[] data = datasets[i].apply(n);
        System.out.printf("Dataset #%d: %s%n", i + 1, labels[i]);

        long t0 = System.nanoTime();
        BitPackedArray compressed = packer.compress(data);
        long t1 = System.nanoTime();

        int samples = Math.min(n, 10_000);
        int sink = 0;
        long g0 = System.nanoTime();
        for (int j = 0; j < samples; j++) {
            int idx = (int) ((long) j * (n - 1) / Math.max(1, samples - 1));
            sink ^= packer.get(compressed, idx);
        }
        long g1 = System.nanoTime();

        int[] restored = packer.decompress(compressed);
        long t2 = System.nanoTime();

        boolean ok = java.util.Arrays.equals(data, restored);

        double tCompressMs = (t1 - t0) / 1e6;
        double tGetMs = (g1 - g0) / 1e6;
        double tGetNs = (g1 - g0) * 1.0 / samples;
        double tDecompressMs = (t2 - g1) / 1e6;
        double sizeBytes = compressed.words().length * 4.0;
        double bitsPerVal = (sizeBytes * 8.0) / n;

        System.out.printf(
            "Words: %d (32-bit) | %.2f bits/val | OK=%s%n",
            compressed.words().length, bitsPerVal, ok ? "✓" : "✗"
        );

        System.out.printf(
            "Time: compress=%.3f ms | get(10k)=%.3f ms (%.1f ns/get) | decompress=%.3f ms%n%n",
            tCompressMs, tGetMs, tGetNs, tDecompressMs
        );
    }

    System.out.println("=== END BENCH ===");
}
}


}