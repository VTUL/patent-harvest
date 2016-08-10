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
        tesseract -l eng $tifimg $tifimg"_ocr" pdf
    done
    
    pdfunite  *.pdf out.pdf
    mv out.pdf "../with_text_"$f.pdf
    
    cd ..
    rm -rf *.tmp


done



