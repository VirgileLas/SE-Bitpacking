# BitPacking Project

Compression d'entiers avec accès direct en Java.

1. Prérequis

---

* JDK 21
* Maven 3.9 ou supérieur

2. Arborescence du projet

---

bitpacking-project/
├─ pom.xml
├─ src/main/java/com/example/bitpacking/
│  ├─ api/BitPacker.java
│  ├─ core/BitPackedArray.java
│  ├─ core/NoSplitBitPacker.java
│  ├─ core/SplitBitPacker.java
│  ├─ core/OverflowBitPacker.java
│  ├─ factory/BitPackerFactory.java
│  └─ cli/Main.java
└─ src/test/java/
├─ BitPackerFactoryTest.java
├─ ConformanceTests.java
├─ RobustnessTests.java
├─ OverflowEdgeTests.java
├─ WordBoundaryTests.java
├─ RandomizedPropertyTests.java
├─ PerformanceSmokeTests.java
└─ IntegrationFactoryModesTest.java


3. Compilation et exécution avec Maven
---
Pour compiler :
mvn clean compile


4. Lancer les tests
---
Exécuter tous les tests JUnit 5 :
mvn clean test

Les rapports se trouvent dans : target/surefire-reports/


5. Lancer les benchmarks
---
Commande générique :
# 1) Benchmark 
mvn -q exec:java -Dexec.mainClass=com.example.bitpacking.cli.Main -Dexec.args="modedecompression --bench"


6.Exécutions courantes avec le CLI:
```powershell

# 2) Total: compresser, décompresser, vérifier, et échantillon de get
mvn -q exec:java -Dexec.mainClass=com.example.bitpacking.cli.Main -Dexec.args="split --total 10,20,30,40,50000,14,152,26,2485,25,24,14"

# 3) Compression: imprime UNIQUEMENT les words (CSV). Ex: "344865" s'il n'y a qu'un mot
mvn -q exec:java -Dexec.mainClass=com.example.bitpacking.cli.Main -Dexec.args="overflow --compress 1,2,3,4,5"

# 4) Sérialisation complète (avec métadonnées) pour faciliter la décompression
mvn -q exec:java -Dexec.mainClass=com.example.bitpacking.cli.Main -Dexec.args="overflow --serialize 1,2,3,4,5"

# 4bis) Décompression depuis la forme sérialisée (sortie de --serialize)
#       Format: n,k,overflowStartBit,kOverflow,overflowCount,kIndex#w0,w1,...
mvn -q exec:java -Dexec.mainClass=com.example.bitpacking.cli.Main -Dexec.args="overflow --decompress 5,2,64,0,0,0#1234,5678"

# 4ter) Décompression depuis des words bruts (il faut fournir les métadonnées)
mvn -q exec:java -Dexec.mainClass=com.example.bitpacking.cli.Main -Dexec.args="overflow --decompress-words 344865 5 4 20 3 0 0"

# 5) Accès direct (get): "n1,n2,.../index"
mvn -q exec:java -Dexec.mainClass=com.example.bitpacking.cli.Main -Dexec.args="nosplit --get 10,20,30,40/2"


7. Modes de compression
---

* NoSplit : pas de chevauchement entre mots 32 bits.
* Split : autorise le chevauchement pour un meilleur ratio.
* Overflow : ajoute une zone overflow pour les valeurs trop grandes.

8. Format de sortie benchmark
---

Chaque test affiche :

* n : taille du dataset
* k : bits utiles
* overflowCount : nombre de valeurs déportées
* words : taille compressée en mots 32 bits
* bits/val : métrique de compression
* compress/get/decompress : temps en ms

9. Conseils de performance
---

* Faire un warm-up JIT avant mesure.
* Répéter les tests et calculer la moyenne.
* Éviter les allocations dans les boucles.
* Pré-calculer masques et tailles dérivées.

10. Auteur
---

Virgile LASSAGNE


