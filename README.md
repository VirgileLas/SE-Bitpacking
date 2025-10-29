# Bit Packing 

Projet en java avec Maven pour une compilation plus facile 

Système de compression d’entiers avec accès direct `get(i)` :
- Variante **sans débordement**
- Variante **avec débordement**
- Variante **avec overflow area** (gros outliers)

## Utilisation 
```bash
mvn -q -DskipTests package
mvn -q exec:java -Dexec.mainClass=com.example.bitpacking.cli.Main -- \
  --mode=nosplit --action=compress --input=datasets/sample.txt
