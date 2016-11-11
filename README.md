This project harvests patent metadata and files from the United States Patent and Trademark Office (USPTO), using the 
[USPTO Open Data Portal](https://developer.uspto.gov/product/patent). The program collect patents assigned
to Virginia Tech for inclusion in [VTechWorks "Virginia Tech Patent" collection](http://vtechworks.lib.vt.edu/handle/10919/72295)
in Virginia Tech's DSpace Instutional repository. 
The metadata fields are crosswalked to fields used in [VTechWorks](http://vtechworks.lib.vt.edu/). 
The program can be modified to search for other assignees and/or harvest other fields.
After harvesting the metadata and files, a script performs OCR on the PDFs and adds that text to each PDF.
 

List of fields in API, http://www.patentsview.org/api/patent.html

additional detail at 
https://git.it.vt.edu/digital-research-services/VTechWorks_Documentation/wikis/Virginia_Tech_Patents

License
-------
This software is licensed under the GNU General Public License v2.

pdfsandwich is licensed under the GNU General Public License v2.
Tobias Elze http://www.tobias-elze.de/pdfsandwich/

## Setup

This is intended to work on Mac OSX. It may work on other platforms if dependencies are installed  via the native package manager. 

#### For CSV creation and PDF harvesting:

`brew install wget`

cd to cloned repo

`wget https://java.net/projects/jsonp/downloads/download/ri/javax.json-ri-1.0.zip`

`unzip javax.json*`

`javac -cp javax.json-ri-1.0/lib/javax.json-1.0.jar Patents.java` (from patent-harvest directory)


---

#### For OCR step:

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


## Run: CSV creation and PDF harvest

cd to cloned repo

`java -cp "javax.json-ri-1.0/lib/javax.json-1.0.jar:." Patents` (from patent-harvest directory)

## Run: Add OCR text to all PDFs

`./text-info-pdf.sh`

Note: This can take a long time - over a minute for a single file. So you might want to run overnight with:

`caffeinate -i ./text-info-pdf.sh`
