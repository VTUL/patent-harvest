USPTO Open Data Portal, https://developer.uspto.gov/product/patent

List of fields in API, http://www.patentsview.org/api/patent.html

additional detail at 
https://git.it.vt.edu/digital-research-services/VTechWorks_Documentation/wikis/Virginia_Tech_Patents

## Setup

cd to cloned repo

For CSV creation and PDF harvesting:

`wget https://java.net/projects/jsonp/downloads/download/ri/javax.json-ri-1.0.zip`

`unzip javax.json*`

`javac -cp javax.json-1.0.jar Patents.java`

---

For OCR step:

`brew install poppler`

`brew install imagemagick`

`brew install libpng`

`brew link libpng`

`brew install tesseract`

`brew install unpaper`

`brew install gawk`

`brew install ocaml`

`brew link ocaml`

`chmod u+x pdfsandwich`


## CSV creation and PDf harvest

cd to cloned repo

`java -cp "javax.json-1.0.jar:." Patents`

## Add OCR'd text to all

`./text-info-pdf.sh`

Note: This can take a long time - over a minute for a single file. So you might want to run overnight with:

`caffeinate -i ./text-info-pdf.sh`