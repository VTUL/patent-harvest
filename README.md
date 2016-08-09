USPTO Open Data Portal, https://developer.uspto.gov/product/patent

List of fields in API, http://www.patentsview.org/api/patent.html

additional detail at 
https://git.it.vt.edu/digital-research-services/VTechWorks_Documentation/wikis/Virginia_Tech_Patents

## Setup

cd to cloned repo

-----
Not needed yet, but will be for OCR

```
brew install poppler

brew install imagemagick

brew link libpng

brew install tesseract

```

-----

`wget https://java.net/projects/jsonp/downloads/download/ri/javax.json-ri-1.0.zip`

`unzip javax.json*`

`javac -cp javax.json-1.0.jar Patents.java`


## Run

cd to cloned repo

`java -cp "javax.json-1.0.jar:." Patents`
