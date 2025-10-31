package com.example.bitpacking.core;

//Data container pour les tableaux compresser pour un simplicité de manipulation 

public class BitPackedArray {
    private final int[] words;  // tableau compressées
    private final int n;        // nombre d'éléments
    private final int k;        // bits par valeur

     // --- Champs utilisés seulement pour Overflow ---
    private final int overflowStartBit; // position (en bits) du 1er overflow
    private final int kOverflow;        // nb de bits par valeur overflow
    private final int overflowCount;    // nombre de valeurs en overflow
    private final int kIndex;           // (optionnel) nb de bits pour indexer la zone overflow

    public BitPackedArray(int[] words, int n, int k) {
        this(words, n, k, 0, 0, 0, 0);
    }
       public BitPackedArray(int[] words, int n, int k, int overflowStartBit, int kOverflow, int overflowCount, int kIndex) {
        this.words = words;
        this.n = n;
        this.k = k;
        this.overflowStartBit = overflowStartBit;
        this.kOverflow = kOverflow;
        this.overflowCount = overflowCount;
        this.kIndex = kIndex;
    }

    public int[] words() { return words; }
    public int n() { return n; }
    public int k() { return k; }
    public int overflowStartBit() { return overflowStartBit; }
    public int kOverflow() { return kOverflow; }
    public int overflowCount() { return overflowCount; }
    public int kIndex() { return kIndex; }
}