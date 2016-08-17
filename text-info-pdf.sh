#!/bin/bash

cd filedump

for f in *.pdf

do
    printf "\n[INFO] Processing file %d\n" "$count"
    ../pdfsandwich -quiet $f
    ((count++))
done



