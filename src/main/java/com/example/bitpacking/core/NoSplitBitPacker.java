package com.example.bitpacking.core;
import com.example.bitpacking.api.BitPacker;

public class NoSplitBitPacker implements BitPacker {

    @Override
    public BitPackedArray compress(int[] tab) {
        // Détermine k, le nombre de bits nécessaire par valeur
        int n = tab.length;
        if (n == 0) {
            // Tableau d'entrée vide -> retour d'un BitPackedArray vide
            return new BitPackedArray(new int[0], 0, 0);
        }
        // Trouver la valeur maximale pour déterminer k
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

        // Calcule le nombre de valeurs par mot de 32 bits et le nombre de mots nécessaires
        int valuesPerWord = 32 / k;                       // arrondi inférieur
        int wordsCount = (n + valuesPerWord - 1) / valuesPerWord;  // arrondi supérieur de n/valuesPerWord
        int[] words = new int[wordsCount];

        // Insère les valeurs dans les mots de sortie sans chevauchement
        int wordIndex = 0;
        int offset = 0;
        for (int val : tab) {
            // Si la valeur ne tient pas dans l'espace restant du mot courant, passer au mot suivant
            if (offset + k > 32) {
                wordIndex++;
                offset = 0;
            }
            // Insérer la valeur à la position de bit courante (décalage de 'offset' )
            words[wordIndex] |= (val << offset);
            offset += k;
            // Si on a exactement rempli le mot, passer au suivant
            if (offset == 32) {
                wordIndex++;
                offset = 0;
            }
        }

        return new BitPackedArray(words, n, k);
    }

    @Override
    public int[] decompress(BitPackedArray compressed) {
        int[] words = compressed.words();
        int n = compressed.n();
        int k = compressed.k();
        int[] output = new int[n];
        if (n == 0) {
            return new int[0];  // rien à décompresser (tableau vide)
        }
        int mask = (k == 32) ? -1 : ((1 << k) - 1); // masque k bits (suite de k 1 permettant de ne retirer après le & que les k bit qu'on veut)

        int valuesPerWord = 32 / k;
        int outputIndex = 0;
        // Parcour chaque mot compressé
        for (int word : words) {
            // Déterminer combien de valeurs sont contenues dans ce mot
            int count = valuesPerWord;
            if (outputIndex + count > n) {
                count = n - outputIndex;  // ajuster pour le dernier mot partiellement rempli
            }
            // Extraire chaque valeur de k bits du mot
            for (int j = 0; j < count; j++) {
                int value = (word >>> (j * k)) & mask;
                output[outputIndex++] = value;
            }
            if (outputIndex >= n) {
                break;  // toutes les valeurs ont été reconstruites
            }
        }
    return output;
    }

    @Override
    public int get(BitPackedArray compressed, int index) {
        int n = compressed.n();
        if (index < 0 || index >= n) {
            throw new IndexOutOfBoundsException("Index invalide: " + index);
        }
        int k = compressed.k();
        int[] words = compressed.words();
        // Calcul du mot et du décalage de bits correspondant à l'index demandé
        int valuesPerWord = 32 / k;
        int wordIndex = index / valuesPerWord;
        int posInWord = index % valuesPerWord;
        int offset = posInWord * k;
        int mask = (k == 32) ? -1 : ((1 << k) - 1); // masque k bits (suite de k 1 permettant de ne retirer après le & que les k bit qu'on veut)
        // Extraction directe de la valeur
        return (words[wordIndex] >>> offset) & mask;
    }
}
