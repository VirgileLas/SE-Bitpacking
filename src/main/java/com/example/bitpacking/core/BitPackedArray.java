package com.example.bitpacking.core;

//Data container pour les tableaux compresser pour un simplicité de manipulation 

public class BitPackedArray {
    private final int[] words;  // tableau compressées
    private final int n;        // nombre d'éléments
    private final int k;        // bits par valeur

    public BitPackedArray(int[] words, int n, int k) {
        this.words = words;
        this.n = n;
        this.k = k;
    }

    public int[] words() { return words; }
    public int n() { return n; }
    public int k() { return k; }
}