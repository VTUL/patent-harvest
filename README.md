USPTO Open Data Portal, https://developer.uspto.gov/product/patent

List of fields in API, http://www.patentsview.org/api/patent.html

additional detail at 
https://git.it.vt.edu/digital-research-services/VTechWorks_Documentation/wikis/Virginia_Tech_Patents

## Setup

cd to cloned repo


_Not needed yet, but will be for OCR_

```
brew install poppler

brew install imagemagick

brew link libpng

brew install tesseract

```

`wget https://java.net/projects/jsonp/downloads/download/ri/javax.json-ri-1.0.zip`

`unzip javax.json*`

`javac -cp javax.json-1.0.jar Patents.java`


## Run

cd to cloned repo

`java -cp "javax.json-1.0.jar:." Patents`

## Add OCR'd text to PDF

`pdfimages -j somefile.pdf ./`

`convert *.pbm -type Grayscale image%d.tif`

`rm -- *pbm`

-----

For each tif

`tesseract -l eng some_image.tif some_pdf_with_text pdf`

----

`/System/Library/Automator/Combine\ PDF\ Pages.action/Contents/Resources/join.py --output all_with_text.pdf *with_text.pdf`

Now all_with_text.pdf is the original pdf, but with text info embedded
