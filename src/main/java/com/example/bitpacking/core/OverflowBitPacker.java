package com.example.bitpacking.core;
import com.example.bitpacking.api.BitPacker;

public class OverflowBitPacker implements BitPacker {
    @Override
    public BitPackedArray compress(int[] tab) {
        int n = tab.length;
        if (n == 0) {
            // Tableau d'entrée vide -> retour d'un BitPackedArray vide
            return new BitPackedArray(new int[0], 0, 0);
        }
        //Calcul de k_overflow = nb de bits du minimum pour représenté la valeur max du tableau (au moins 1 bit) et donc ici notre valeur pour l'overflow 
        int max = 0;
        for (int val : tab) {
            if (val > max) {
                max = val;
            }
        }
        int k_overflow;
        if (max == 0){
            k_overflow= 1;}
        else k_overflow = 32 - Integer.numberOfLeadingZeros(max); // taille en bits pour stocker l'overflow 
    
    
        // Recherche du meilleur k_small par balayage de 1 à k_overflow donc max 32
        long bestCost = Long.MAX_VALUE;
        int best_k_small = 1; // le plus petit nombre de bits pour les valeur non overflow 
        int best_k_index = 0; // le plus petit nombre de bits pour nécaissaire pour représenter les valeurs overflow 
        int best_k_payload = 0; // la taille commune max(k_index, k_small)
        int best_k_total = 0; // taille en bits du payload plus le flag (k_payload +1)
        int best_m = 0; //nombe de valeur en overflow 
        for (int s = 1; s <= k_overflow; s++) {
            long threshold = ((1L << s) - 1); // la plus grande valeur que tu peux écrire sur s bit
            int m = 0;
            // Compter les éléments > threshold 
            for (int val : tab) {
                if ((long) val > threshold) {     // valeur qui ne tient pas en s bits
                    m++;
                }
            }
            // Calcul de k_index, k_payload, k_total et k_overflow pour ce s
            int k_index;
            if (m <= 1) {
                k_index = 0;
            } else {k_index = 32 - Integer.numberOfLeadingZeros(m - 1);}
            int k_payload;
            if(s > k_index) {
                k_payload = s;
            } else {k_payload = k_index;}
            int k_total = 1 + k_payload;
            // Calcul du coût total en bits pour ce choix
            long cost = (long) n * k_total + (long) m * k_overflow; // fonction de calcul de couts combien coute en bits l'implémentation avec se k_total
            if (cost < bestCost) {
                // Mettre à jour le meilleur choix
                bestCost = cost;
                best_k_small = s;
                best_k_index = k_index;
                best_k_payload = k_payload;
                best_k_total = k_total;
                best_m = m;
            }
        }
    
        //Compression en utilisant les paramètres optimaux trouvés
        int k_small = best_k_small;
        int k_index = best_k_index;
        int k_payload = best_k_payload;
        int k_total = best_k_total;
        int m = best_m;
        long threshold = ((1L << k_small) - 1);
    
        // Allouer le tableau de mots de sortie d'une taille suffisante pour les deux zones
        int valuesPerWordMain = 32 / k_total;
        int mainWordsCount = (n + valuesPerWordMain - 1) / valuesPerWordMain;
        int valuesPerWordOvfl = (k_overflow == 0) ? 0 : (32 / k_overflow);
        int ovflWordsCount = (m == 0) ? 0 : ((m + valuesPerWordOvfl - 1) / valuesPerWordOvfl);
        int[] words = new int[mainWordsCount + ovflWordsCount];
    
        // Insére les éléments dans la zone principale (flag + payload)
        int wordIndex = 0;
        int offset = 0;
        int[] overflowValues = new int[m];   // stocke les valeurs overflow à part
        int overflowPos = 0;
        for (int val : tab) {
            boolean isOverflow = ((long) val > threshold);
            int flag;
            if(isOverflow){flag = 1;
            } else {flag = 0;}
            int payload;
            if (isOverflow) {
                payload = overflowPos;
                overflowValues[overflowPos++] = val;   // ajoute la valeur à la liste overflow
            } else {
                payload = val;
            }
            // Combine flag et payload en un entier de k_total bits (flag en bit de poids fort)
            int entryBits = (flag << k_payload) | (payload & ((1 << k_payload) - 1));
            // Si le bloc ne tient pas dans l'espace restant, passer au mot suivant
            if (offset + k_total > 32) {
                wordIndex++;
                offset = 0;
            }
            // Placer le bloc à la position courante (décalage offset bits)
            words[wordIndex] |= (entryBits << offset);
            offset += k_total;
            if (offset == 32) {
                wordIndex++;
                offset = 0;
            }
        }
        // Mémoriser la position de début de la zone overflow (en bits)
        int overflowStartBit = wordIndex * 32 + offset;
    
        // Insére les valeurs de la zone overflow, en NoSplit 
        for (int i = 0; i < m; i++) {
            int val = overflowValues[i];
            // Préparer la valeur sur k_overflow bits (on masque par sécurité)
            long vBits = val & ((k_overflow == 32) ? -1 : ((1L << k_overflow) - 1)); // important de faire la vérification == 32 car on va covertir en int ce long donc si on dépasse la taille d'un int incompatible 
            if (offset + k_overflow > 32) {
                wordIndex++;
                offset = 0;
            }
            // Placer la valeur overflow par rapport à l'offset
            words[wordIndex] |= (int) (vBits << offset);
            offset += k_overflow;
            if (offset == 32) {
                wordIndex++;
                offset = 0;
            }
        }   
        return new BitPackedArray(words, n, k_total, overflowStartBit, k_overflow, m, k_index);
    }
    
    

@Override
public int[] decompress(BitPackedArray compressed) {

    int n = compressed.n();                        // nombre total d'éléments
    int[] words = compressed.words();              // tableau de mots 32 bits
    int k_total = compressed.k();                  // taille (flag + payload)
    int k_payload = k_total - 1;                   // taille du champ de données
    int k_overflow = compressed.kOverflow();       // taille d'une valeur overflow
    int overflowStartBit = compressed.overflowStartBit(); // position de début overflow (en bits)

    int[] result = new int[n];// Tableau résultat

    int wordIndex = 0;     // mot courant dans words[]
    int bitOffset = 0;     // position du prochain bit à lire
    int overflowIndex = 0; // index dans la zone overflow (compteur séquentiel)

    for (int i = 0; i < n; i++) {

        if (bitOffset + k_total > 32) {
            bitOffset = 0;
            wordIndex++;
        }

        // Lecture du bloc de k_total bits (flag + payload)
        int code = (words[wordIndex] >>> bitOffset) & ((1 << k_total) - 1);

        // Extraction du flag et du payload
        int flag = code >>> k_payload;                  // bit de poids fort = flag
        int payload = code & ((1 << k_payload) - 1);    // reste = valeur ou index overflow

        // Avancer le pointeur
        bitOffset += k_total;
        if (bitOffset >= 32) {
            bitOffset = 0;
            wordIndex++;
        }

        if (flag == 0) {
            // Valeur directe : payload = valeur
            result[i] = payload;
        } else {
            // Valeur overflow :
            int index = overflowIndex;  

            // Position dans la zone overflow (NOSPLIT) — calcul par “slots”
            int startWord   = (int) (overflowStartBit / 32);  
            int startOffset = (int) (overflowStartBit % 32);   
            int valuesPerWord = 32 / k_overflow;               

            int overflowWord, overflowOffset;

            if (startOffset == 0) {
                // Aligné : index se mappe en (word,offset) par slots pleins
                overflowWord   = startWord + (index / valuesPerWord);
                overflowOffset = (index % valuesPerWord) * k_overflow;
            } else {
                // Non aligné : combien de valeurs tiennent dans le premier mot ?
                int firstFit = (32 - startOffset) / k_overflow; // floor

                if (index < firstFit) {
                    // Toujours dans le premier mot
                    overflowWord   = startWord;
                    overflowOffset = startOffset + index * k_overflow;
                } else {
                    // On a consommé le premier mot, repars mot suivant à offset 0
                    int rest = index - firstFit;
                    overflowWord   = startWord + 1 + (rest / valuesPerWord);
                    overflowOffset = (rest % valuesPerWord) * k_overflow;
                }
            }
            if (overflowWord >= words.length) {
                throw new IllegalStateException("Dépassement du tableau words à l’index " + overflowWord);
            }

            // Lecture (en no-split → tient toujours dans 1 seul mot)
            int mask = (k_overflow == 32) ? -1 : ((1 << k_overflow) - 1);
            int value = (words[overflowWord] >>> overflowOffset) & mask;
            result[i] = value;

            overflowIndex++; // on avance dans la zone overflow
        }
    }
    return result;
}




@Override
public int get(BitPackedArray compressed, int index) {
    int n = compressed.n();
    if (index < 0 || index >= n) {
        throw new IndexOutOfBoundsException(index + " est hors limite de " + n);
    }

    int[] words = compressed.words();
    int kTotal   = compressed.k();            // flag (1) + payload
    int kPayload = kTotal - 1;
    int kOverflow = compressed.kOverflow();   // bits par valeur overflow
    int overflowCount = compressed.overflowCount();
    int overflowStartBit = compressed.overflowStartBit();

    // Chaque entrée "flag+payload" a une largeur fixe kTotal et ne chevauche pas les mots.
    int valuesPerWordMain = 32 / kTotal;                 
    int mainWordIndex = index / valuesPerWordMain;
    int mainBitOffset = (index % valuesPerWordMain) * kTotal;

    // Lit le bloc kTotal bits (flag+payload)
    int codeMask = (kTotal == 32) ? -1 : ((1 << kTotal) - 1); // ici on reste en int alors on a aussi besoin de l'execption k_total=32
    int code = (words[mainWordIndex] >>> mainBitOffset) & codeMask;

    // Découpe flag et payload
    int flag = code >>> kPayload;                        // on sort le bit de poid fort 
    int payloadMask = ((1 << kPayload) - 1);
    int payload = code & payloadMask;

    if (flag == 0) { // pas une valeur overflow 
        return payload; // on renvoie direct la valeur 
    } else { // une valeur dans l'overflow
    int overflowIndex = payload;

    if (overflowIndex < 0 || overflowIndex >= overflowCount) {
        // Sécurité utile si le flux est corrompu
        throw new IllegalStateException(
            "Overflow index hors bornes: " + overflowIndex + " / " + overflowCount
        );
    }
    int startWord   = (int) (overflowStartBit >>> 5);  // /32
    int startOffset = (int) (overflowStartBit & 31);   // %32
    int valuesPerWord = 32 / kOverflow;

    int overflowWord, overflowOffset;
    if (startOffset == 0) {
        overflowWord   = startWord + (overflowIndex / valuesPerWord);
        overflowOffset = (overflowIndex % valuesPerWord) * kOverflow;
    } else {
        int firstFit = (32 - startOffset) / kOverflow;
        if (overflowIndex < firstFit) {
            overflowWord   = startWord;
            overflowOffset = startOffset + overflowIndex * kOverflow;
        } else {
            int rest = overflowIndex - firstFit;
            overflowWord   = startWord + 1 + (rest / valuesPerWord);
            overflowOffset = (rest % valuesPerWord) * kOverflow;
            }
    }

    int ovMask = (kOverflow == 32) ? -1 : ((1 << kOverflow) - 1); // ici on reste en int alors on a aussi besoin de l'execption kOverflow=32
    int value = (words[overflowWord] >>> overflowOffset) & ovMask;

    return value;
    }
}

}
