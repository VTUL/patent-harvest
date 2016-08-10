#!/bin/bash

cd filedump
for f in *.pdf

do

    mkdir $f".tmp"
    cd $f".tmp"
    pdfimages -j "../"$f ./
    convert \*.pbm -type Grayscale \%d.tif
    rm -- \*.pbm
    
    for tifimg in *.tif
    do
        tesseract -l eng $tifimg $tifimg pdf
    done
    
    pdfunite  $(ls *.pdf | sort -n) output.pdf
    mv output.pdf "../with_text_"$f
    
    cd ..
    rm -rf *.tmp


done



