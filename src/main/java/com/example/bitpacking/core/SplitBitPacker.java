package com.example.bitpacking.core;
import com.example.bitpacking.api.BitPacker;


public class OverflowBitPacker implements BitPacker {
    
@Override
    public BitPackedArray compress(int[] tab) {
        int n = tab.length;
        // Calcul de k = nombre de bits nécessaires pour représenter la valeur maximale
        int max = 0;
        for (int val : tab) {
            if (val > max) {
                max = val;
            }
        }
        // Assurer au moins 1 bit si max == 0.
        int k;
        if (max == 0){
            k= 1;}
        else k = 32 - Integer.numberOfLeadingZeros(max);


        // Calcul du nombre total de bits et du nombre de mots 32 bits nécessaires
        long totalBits = (long) n * k; //un long parce que n*k peut dépasser la taille d'un int /!\
        int wordCount = (int) ((totalBits + 31) / 32);
        int[] words = new int[wordCount];

        // Insertion des bits de chaque entier dans le buffer words
        long bitPos = 0;  // position binaire globale où écrire le prochain entier

        for (int i = 0; i < n; i++) {
            // Limiter la valeur à k bits (par sécurité, au cas où value >= 2^k)
            long val = tab[i] & ((1L << k) - 1); // ici aussi on utilise un long pour palier au cas limite de k=32 car sinon pendant shift gauche on aurait dépassé
            // Calcul de l'index de mot et du décalage de bit à l'intérieur de ce mot
            int wordIndex = (int) (bitPos /32);
            int bitOffset = (int) (bitPos % 32);
            // Nombre de bits restant dans le mot courant à partir de bitOffset
            int bitsInCurrentWord = 32 - bitOffset;

            if (bitsInCurrentWord >= k) {
                // Tous les k bits tiennent dans le mot courant
                words[wordIndex] |= (int) (val << bitOffset);
            } else {
                int lowBits = bitsInCurrentWord;
                int highBits = k - lowBits;

                long lowPart = (val & (1L << k) - 1) & ((1L << lowBits) - 1);
                long highPart = (val & (1L << k) - 1) >>> lowBits;

                words[wordIndex] |= (int) (lowPart << bitOffset);
                words[wordIndex + 1] |= (int) highPart;
            }

            bitPos += k;
        }

        // Construction de l'objet BitPackedArray (contient words, n, k)
        BitPackedArray packed = new BitPackedArray(words, n, k);

        return packed;
    }

@Override
    public int[] decompress(BitPackedArray compressed) {
        int[] words = compressed.words();
        int n = compressed.n();
        int k = compressed.k();


        int[] output = new int[n];
        long bitPos = 0;
        for (int i = 0; i < n; i++) {
            int wordIndex = (int) (bitPos /32);
            int bitOffset = (int) (bitPos % 32);

            if (bitOffset + k <= 32) {
            // Tout tient dans un seul mot
                int word = words[wordIndex];
                int mask = (k == 32) ? -1 : (1 << k) - 1;
                output[i] = (word >>> bitOffset) & mask;
            } else {
            // La valeur est à cheval sur deux mots même principe que dans get
                int bitsInCurrentWord = 32 - bitOffset;
                int maskLow = (1 << bitsInCurrentWord) - 1;
                int lower = (words[wordIndex] >>> bitOffset) & maskLow;

                int remainingBits = k - bitsInCurrentWord;
                int maskHigh = (1 << remainingBits) - 1;
                int upper = words[wordIndex + 1] & maskHigh;

                output[i] = lower | (upper << bitsInCurrentWord);
            }
            bitPos += k;
        }
    return output;
    }
@Override
    public int get(BitPackedArray compressed, int index) {
        int k = compressed.k();
        int[] words = compressed.words();

    // Position de départ en bits de l'élément indexé
        long bitStart = (long) index * k;
        int wordIndex = (int) (bitStart /32);
        int bitOffset = (int) (bitStart %32);

        // Extraction des k bits à cheval sur words[wordIndex] (et wordIndex+1 si nécessaire)
        if (bitOffset + k <= 32) {
            // Cas simple: tous les bits sont dans le même mot
            int word = words[wordIndex];
            int mask = (k == 32) ? -1 : ((1 << k) - 1);   // masque k bits 
            return (word >>> bitOffset) & mask;
        } else {
            // Cas de chevauchement sur deux mots
            int bitsInCurrentWord = 32 - bitOffset;
            int maskLow = (1 << bitsInCurrentWord) - 1;
            int lowerBits = (words[wordIndex] >>> bitOffset) & maskLow;
            int remainingBits = k - bitsInCurrentWord;
            int maskHigh = (remainingBits == 32) ? -1 : ((1 << remainingBits) - 1);
            int upperBits = words[wordIndex + 1] & maskHigh;
            return lowerBits | (upperBits << bitsInCurrentWord);
        }
    }

}
