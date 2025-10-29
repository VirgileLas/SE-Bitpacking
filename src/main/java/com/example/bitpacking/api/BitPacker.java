package com.example.bitpacking.api;
import com.example.bitpacking.core.BitPackedArray;

public interface BitPacker {
    /**
     * Compresse le tableau source et retourne un objet BitPackedArray contenant les données compressées.
     */
    BitPackedArray compress(int[] input);

    /**
     * Décompresse un BitPackedArray vers un tableau d’entiers.
     */
    int[] decompress(BitPackedArray compressed);

    /**
     * Accès direct au i-ème entier compressé dans le tableau.
     */
    int get(BitPackedArray compressed, int index);
}
