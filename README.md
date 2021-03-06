This project harvests patent metadata and files from the United States Patent and Trademark Office (USPTO), using the 
[USPTO Open Data Portal](https://developer.uspto.gov/product/patent). The program collect patents assigned
to Virginia Tech for inclusion in [VTechWorks "Virginia Tech Patent" collection](http://vtechworks.lib.vt.edu/handle/10919/72295)
in Virginia Tech's DSpace institutional repository. 
The metadata fields are crosswalked to fields used in [VTechWorks](http://vtechworks.lib.vt.edu/). 
This program can be modified to search for other assignees and/or harvest other fields.
After harvesting the metadata and files, a script performs OCR on the PDFs and adds that text to each PDF.
 

List of fields in API, http://www.patentsview.org/api/patent.html

Additional detail at 
https://git.it.vt.edu/digital-research-services/VTechWorks_Documentation/wikis/Virginia_Tech_Patents (restricted to VTUL members)

Overview of project: https://blogs.lt.vt.edu/openvt/2017/06/02/introducing-the-virginia-tech-patents-collection-in-vtechworks-and-the-patent-harvesting-software-repository-patent-harvest/

License
-------
This software is licensed under the GNU General Public License v2.

pdfsandwich is licensed under the [GNU General Public License v2](http://www.tobias-elze.de/pdfsandwich/).

## Installation

This is intended to work on Mac OSX. It may work on other platforms if dependencies are installed  via the native package manager. 
All of the following steps are from the Mac Terminal.

#### For CSV creation and PDF harvesting:
install wget to download this project and pdfsandwich
```
brew install wget
```

cd to the directory that will contain the cloned repo
download this project and unzip it
```
wget https://java.net/projects/jsonp/downloads/download/ri/javax.json-ri-1.0.zip
unzip javax.json*
```

cd to the patent-harvest directory
```
javac -cp javax.json-ri-1.0/lib/javax.json-1.0.jar Patents.java
```

---

#### To install the OCR software:

```
brew install poppler
brew install imagemagick
brew install libpng
brew link libpng
brew install tesseract
brew install unpaper
brew install gawk
brew install ocaml
brew link ocaml
chmod u+x pdfsandwich
```

## Run: CSV creation and PDF harvest

cd to patent-harvest directory
```
java -cp "javax.json-ri-1.0/lib/javax.json-1.0.jar:." Patents
```

## Run: Add OCR text to all PDFs

```
./text-info-pdf.sh`
```

Note: This can take a long time - over a minute for a single file. So you might want to run overnight with:
```
caffeinate -i ./text-info-pdf.sh
```
