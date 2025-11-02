package com.example.bitpacking.cli;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.IntFunction;

import com.example.bitpacking.api.BitPacker;
import com.example.bitpacking.core.*;
import com.example.bitpacking.factory.BitPackerFactory;

public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsageAndExit();
        }

        final String mode = args[0];

        // ---- Factory: un seul paramètre, retourne la bonne implémentation
        final BitPacker packer;
        try {
            packer = BitPackerFactory.create(mode);
        } catch (Exception e) {
            System.err.println("Factory error: " + e.getMessage());
            System.exit(2);
            return; // pour le compilateur
        }

        // Commande/action
        final String action = args[1].toLowerCase(Locale.ROOT);

        try {
            switch (action) {
                case "--bench":
                    runBenchmark(packer);
                    break;
                case "--total": {
                    if (args.length < 3) {
                        System.err.println("Manque la suite de nombres pour --total.");
                        printUsageAndExit();
                    }
                    int[] input = parseCsvInts(args[2]);
                    runTotalFlow(mode, packer, input);
                    break;
                }
                case "--compress": {
                    if (args.length < 3) {
                        System.err.println("Manque la suite de nombres pour --compress.");
                        printUsageAndExit();
                    }
                    int[] input = parseCsvInts(args[2]);
                    BitPackedArray packed = packer.compress(input);
                    // Sortie demandée: uniquement les words (CSV). Ex: "344865" s'il n'y a qu'un mot.
                    System.out.println(joinCsv(packed.words()));
                    break;
                }
                case "--serialize": { // variante: sortie complète sérialisée (avec métadonnées) pour faciliter --decompress
                    if (args.length < 3) {
                        System.err.println("Manque la suite de nombres pour --serialize.");
                        printUsageAndExit();
                    }
                    int[] input = parseCsvInts(args[2]);
                    BitPackedArray packed = packer.compress(input);
                    String serialized = serializePacked(packed);
                    System.out.println(serialized);
                    break;
                }
                case "--decompress": {
                    if (args.length < 3) {
                        System.err.println("Manque les données compressées pour --decompress.");
                        printUsageAndExit();
                    }
                    BitPackedArray packed = deserializePacked(args[2]);
                    int[] restored = packer.decompress(packed);
                    System.out.println(joinCsv(restored));
                    break;
                }
                case "--decompress-words": { // permet de fournir les words + métadonnées manuellement
                    // Usage: <mode> --decompress-words <wordsCsv> <n> <k> [overflowStartBit kOverflow overflowCount kIndex]
                    if (args.length != 5 && args.length != 9) {
                        System.err.println("Usage: <mode> --decompress-words <w0,w1,...> <n> <k> [overflowStartBit kOverflow overflowCount kIndex]");
                        System.exit(1);
                    }
                    int[] words = parseCsvInts(args[2]);
                    int n = Integer.parseInt(args[3]);
                    int k = Integer.parseInt(args[4]);
                    BitPackedArray packed;
                    if (args.length == 5) {
                        packed = new BitPackedArray(words, n, k);
                    } else {
                        int overflowStartBit = Integer.parseInt(args[5]);
                        int kOverflow = Integer.parseInt(args[6]);
                        int overflowCount = Integer.parseInt(args[7]);
                        int kIndex = Integer.parseInt(args[8]);
                        packed = new BitPackedArray(words, n, k, overflowStartBit, kOverflow, overflowCount, kIndex);
                    }
                    int[] restored = packer.decompress(packed);
                    System.out.println(joinCsv(restored));
                    break;
                }
                case "--get": {
                    if (args.length < 3) {
                        System.err.println("Format attendu pour --get: \"n1,n2,.../index\"");
                        printUsageAndExit();
                    }
                    GetRequest req = parseGetRequest(args[2]);
                    BitPackedArray packed = packer.compress(req.values);
                    int val = packer.get(packed, req.index);
                    System.out.println(val);
                    break;
                }
                default:
                    // Compat: si l'utilisateur passe directement la suite de nombres sans action, on exécute le flux total (ancien comportement)
                    int[] input = parseCsvInts(args[1]);
                    runTotalFlow(mode, packer, input);
                    break;
            }
        } catch (IllegalArgumentException ex) {
            System.err.println("Erreur d'arguments: " + ex.getMessage());
            System.exit(3);
        }
    }

    // ---- Flux "total": compresser, décompresser, vérifier + échantillon de get
    private static void runTotalFlow(String mode, BitPacker packer, int[] input) {
        long t0 = System.nanoTime();
        BitPackedArray compressed = packer.compress(input);
        long t1 = System.nanoTime();

        int[] restored = packer.decompress(compressed);
        long t2 = System.nanoTime();

        System.out.println("Mode        : " + mode);
        System.out.println("Input size  : " + input.length);
        System.out.println("Input (head): " + head(input, 16));
        System.out.println();
        System.out.println("Compressed:");
        System.out.println("  n = " + compressed.n() + " | k = " + compressed.k() + " | words.length = " + compressed.words().length);
        if (compressed.overflowCount() > 0 || compressed.kOverflow() > 0) {
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

        System.out.printf("Timing      : compress=%.3f ms | decompress=%.3f ms%n", (t1 - t0) / 1e6, (t2 - t1) / 1e6);

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
                boolean okGet = (got == input[idx]);
                if (!okGet) allOk = false;
                System.out.printf("  get(%d) = %d (%s) | %.1f ns%n", idx, got, okGet ? "ok" : "not ok", (t4 - t3) / 1e3);
            }
            System.out.println(allOk ? "échantillon OK." : "divergences détectées.");
        }
    }

    // ---- Parsing/utilitaires ----
    private static int[] parseCsvInts(String csv) { // Verifie que le tableau est bien conforme au règle imposé entier positif et pas de valeur null
        String[] parts = csv.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim();
            if (s.isEmpty()) throw new IllegalArgumentException("Valeur vide " + i);
            int v = Integer.parseInt(s);
            if (v < 0) throw new IllegalArgumentException("Pas de négatif autorisé " + v);
            arr[i] = v;
        }
        return arr;
    }

    private static String joinCsv(int[] a) {
        StringBuilder sb = new StringBuilder(Math.max(16, a.length * 2));
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(a[i]);
        }
        return sb.toString();
    }

    // Format sérialisé sans caractère spécial PowerShell: "n,k,overflowStartBit,kOverflow,overflowCount,kIndex#w0,w1,..."
    private static String serializePacked(BitPackedArray p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.n()).append(',')
          .append(p.k()).append(',')
          .append(p.overflowStartBit()).append(',')
          .append(p.kOverflow()).append(',')
          .append(p.overflowCount()).append(',')
          .append(p.kIndex())
          .append('#');
        int[] w = p.words();
        for (int i = 0; i < w.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(w[i]);
        }
        return sb.toString();
    }

    private static BitPackedArray deserializePacked(String s) {
        int sep = s.indexOf('#');
        if (sep <= 0) throw new IllegalArgumentException("Format compressé invalide. Attendu: n,k,overflowStartBit,kOverflow,overflowCount,kIndex#w0,w1,...");
        String header = s.substring(0, sep);
        String wordsCsv = s.substring(sep + 1);
        String[] h = header.split(",");
        if (h.length != 6) throw new IllegalArgumentException("Entête invalide: 6 champs attendus.");
        int n = Integer.parseInt(h[0].trim());
        int k = Integer.parseInt(h[1].trim());
        int overflowStartBit = Integer.parseInt(h[2].trim());
        int kOverflow = Integer.parseInt(h[3].trim());
        int overflowCount = Integer.parseInt(h[4].trim());
        int kIndex = Integer.parseInt(h[5].trim());
        int[] words = (wordsCsv.trim().isEmpty()) ? new int[0] : parseCsvInts(wordsCsv);
        return new BitPackedArray(words, n, k, overflowStartBit, kOverflow, overflowCount, kIndex);
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
        return s.substring(0, 8) + "_" + s.substring(8, 16) + "_" + s.substring(16, 24) + "_" + s.substring(24, 32);
    }

    private static GetRequest parseGetRequest(String arg) {
        int slash = arg.lastIndexOf('/');
        if (slash <= 0 || slash == arg.length() - 1) {
            throw new IllegalArgumentException("Format --get invalide. Utiliser: \"1,2,3,4/2\" (index=2)");
        }
        String csv = arg.substring(0, slash).trim();
        String idxStr = arg.substring(slash + 1).trim();
        int[] vals = parseCsvInts(csv);
        int idx = Integer.parseInt(idxStr);
        if (idx < 0 || idx >= vals.length) {
            throw new IllegalArgumentException("Index hors bornes pour le tableau fourni.");
        }
        return new GetRequest(vals, idx);
    }

    private static void printUsageAndExit() {
        System.err.println("Usage:");
        System.err.println("  <mode> --bench");
        System.err.println("  <mode> --total <n1,n2,n3,...>");
        System.err.println("  <mode> --compress <n1,n2,n3,...>    -> imprime seulement les words compressés (CSV)");
        System.err.println("  <mode> --serialize <n1,n2,n3,...>   -> imprime un format sérialisé complet (avec métadonnées)");
        System.err.println("  <mode> --decompress <format-serial> -> restitue la liste d'entiers (utiliser la sortie de --serialize)");
        System.err.println("  <mode> --decompress-words <w0,w1,...> <n> <k> [overflowStartBit kOverflow overflowCount kIndex]");
        System.err.println("  <mode> --get \"n1,n2,.../index\"     -> renvoie la valeur à l'index via get()");
        System.err.println("Modes: split | nosplit | overflow");
        System.err.println("Format sérialisé (--serialize): n,k,overflowStartBit,kOverflow,overflowCount,kIndex#w0,w1,... (sans espaces)");
        System.exit(1);
    }

    // Petit container pour --get
    private static final class GetRequest {
        final int[] values;
        final int index;
        GetRequest(int[] values, int index) { this.values = values; this.index = index; }
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

        final int[] sizes = { 1_000, 100_000, 1_000_000 };
        for (int s = 0; s < sizes.length; s++) {
            int n = sizes[s];
            System.out.printf("%n--- Taille tableau : %d éléments ---%n", n);
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
                        compressed.words().length, bitsPerVal, ok ? "✓" : "✗");

                System.out.printf(
                        "Time: compress=%.3f ms | get(10k)=%.3f ms (%.1f ns/get) | decompress=%.3f ms%n%n",
                        tCompressMs, tGetMs, tGetNs, tDecompressMs);
            }

            System.out.println("=== END BENCH ===");
        }
    }
}