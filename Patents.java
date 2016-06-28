/*
 *  
 *  You need to install WGET: `brew install wget`
 *  Need to change patentCount to be > number of all VT patents, including pending ones
 *  Compile with: `javac Patents.java`
 *  Run with: `java Patents`
 *      Program will output PatentMetadata.csv
 *      and download all PDF's [patentNumber].pdf
 *      the directory `filedump`
 * 
 *  Philippe Gray
 * 
 * 
 * */

import java.util.*;
import javax.xml.*;
import javax.xml.parsers.*;
import java.io.*;
import java.nio.file.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.net.*;

public class Patents {
    
    public static void main(String[] args) throws SAXException, IOException,
    ParserConfigurationException, InterruptedException {
        
        /******************** Config stuff ********************/
        
        /* xmlFile came from this url:
         * This is the most up to date source. 
         * Change the "rows" parameter in URL to get that many entries
         */
         
         
        /*
         * The following three depend on the USPTO not changing their 
         * website structure or things will likely break
         *
         */
         
        /*Path to XML file */
        String xmlPath1 = "http://prod-proxy-lb-2117675230.us-east-1.elb.amazonaws.com/solr/aotw/search?json.wrf=jQuery11020939095123326368_1466534432642&q=virginia+tech&facet.date.other=before&rows=";
        String xmlPath2 = "&start=0&wt=xml&fq=patAssigneeNameFacet%3A%22VIRGINIA+TECH+INTELLECTUAL+PROPERTIES%2C+INC.%22&facet.date.start=NOW%2FYEAR-50YEARS&fl=id%2CreelNo%2CframeNo%2CconveyanceText%2CpatAssigneeName%2CpatAssignorName%2CinventionTitleFirst%2CapplNumFirst%2CpublNumFirst%2CpatNumFirst%2CintlRegNumFirst%2CcorrName%2CcorrAddress1%2CcorrAddress2%2CcorrAddress3%2CpatAssignorEarliestExDate%2CfilingDateFirst%2CpublDateFirst%2CissueDateFirst%2CintlPublDateFirst%2CpatNumSize&hl.fl=reelNo%2CframeNo%2CpatAssigneeName%2CpatAssignorName%2CconveyanceText%2CinventionTitleFirst%2CapplNumFirst%2CpublNumFirst%2CpatNumFirst%2CintlRegNumFirst%2CcorrName%2CcorrAddress1%2CcorrAddress2%2CcorrAddress3&hl.requireFieldMatch=true&sort=patAssignorEarliestExDate+desc%2C+id+desc";
        int patentCount = 800;
        String xmlFile = "PatentMetadata.xml";
        
        /* Path to PDF's */
        String usptoPDFPath = "http://pimg-fpiw.uspto.gov/fdd/";
        
        /* This is used to get fields not present in xml file e.g. inventor names */
        String apiBaseUrl = "http://www.patentsview.org/api/patents/query?";
        String[] additionalFields = {"inventor_first_name", "inventor_last_name", "patent_abstract"};
        
        /* Download every patent pdf? Will take a while if set to true */
        boolean getPDFs = true;
        
        /*****************************************************/
        
        
        ArrayList<Doc> database = new ArrayList<Doc>();
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
        .newInstance();
     
        
        /* Download the XML file */
        System.out.println("Downloading XML entries for " + patentCount + " parent(s)");
        StringBuilder XmlSb = new StringBuilder("wget -O PatentMetadata.xml ");
        XmlSb.append(xmlPath1);
        XmlSb.append(Integer.toString(patentCount));
        XmlSb.append(xmlPath2);
        Process p = Runtime.getRuntime().exec(XmlSb.toString());
        p.waitFor();
        
        /* Extract info from XML file */
        DocumentBuilder docBuilder;
        FileInputStream inStream;
        Document document;
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
            inStream = new FileInputStream(xmlFile);
            document = docBuilder.parse(inStream, "UTF-8");
        }
        catch (Exception e) {
            System.out.println("Error: You need to decrease `patentCount` setting, as you " +
            "specified more than the number that exists on USPTO's website");
            return;
            
        }
        
        Element response = (Element)document.getElementsByTagName("response").item(0);
        Element result = (Element)document.getElementsByTagName("result").item(0);
        NodeList entries = result.getChildNodes();
        System.out.println("Extracting metadata for " + entries.getLength() + " patent(s)");
        for (int i = 0; i < entries.getLength(); ++i) {
            Node entry = entries.item(i);
            Doc myDoc = new Doc();
            
            NodeList fields = entry.getChildNodes();
            for (int j = 0; j < fields.getLength(); ++j) {
                Node field = fields.item(j);
                String fieldName = ((Element)field).getAttribute("name");
                myDoc.addEntry(fieldName, field.getTextContent());
                if (field.getChildNodes().getLength() > 1) {
                    NodeList subFields = field.getChildNodes();
                    for (int k = 0; k < subFields.getLength(); ++k) {
                        Node subField = subFields.item(k);
                        myDoc.addEntry(((Element)field).getAttribute("name"), subField.getTextContent());
                    }
                }
                
            }
            
            /* Getting additional info from API */
            String patNum = myDoc.getEntry("patNumFirst");
            if (!patNum.equals("NULL")) {
                Document apiXmlDoc = xmlFromApi(apiBaseUrl, patNum ,
                    additionalFields);
            
                for (String fieldName : additionalFields) {
                      NodeList fieldVals = apiXmlDoc.getElementsByTagName(fieldName);
                      for (int k = 0; k < fieldVals.getLength(); ++k) {
                        myDoc.addEntry(fieldName, fieldVals.item(k).getTextContent());
                      }
                      
                }
                
            }
            myDoc.joinEntries( "inventors_last_first", 
                new String[] {"inventor_last_name", "inventor_first_name"}, "\\|\\|", ",", true);
            
            
            
            if (myDoc.getEntry("patAssigneeName").contains("VIRGINIA TECH") &&
                !myDoc.getEntry("patNumFirst").equals("NULL")) {
                database.add(myDoc);
            }
        }
        printCSV(database, "./VTPatents.csv");
   
   
        if (getPDFs) {
            /* Download PDFs */
            Runtime.getRuntime().exec("mkdir ./filedump");
            for (Doc doc : database) {
                StringBuilder sb = new StringBuilder("wget  -O filedump/" + doc.getEntry("patNumFirst") + ".pdf");
                // Url is based on pat number
                
                sb.append(" ");
                sb.append(usptoPDFPath);
                
                String baseUrl = sb.toString();
                String patNumName = getUsptoFilename(doc.getEntry("patNumFirst"));
                sb.append(patNumName);
                System.out.println("GET " + doc.getEntry("patNumFirst") + ".pdf");
                p = Runtime.getRuntime().exec(sb.toString());
                p.waitFor();
                

            }
        }
        
    }
    
    
    
    
    
    // Weird conversion of ID to filename
    public static String getUsptoFilename(String id) {
        id = id.replace("-", "");
        StringBuilder idSb = new StringBuilder(id);
        int firstNumIndex = -1;
        for (int i = 0; i < id.length(); ++i) {
        
            if(Character.isLetter(id.charAt(i))) {
                firstNumIndex = i;
            }
        }
        
        while(idSb.length() < 8) {
            idSb.insert(firstNumIndex + 1, "0");
        }
        id = idSb.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(id.substring(id.length() - 2));
        sb.append("/");
    
        sb.append(id.substring(3,6));
        sb.append("/");
        sb.append(id.substring(0,3));
        
     
        sb.append("/0.pdf");
        return sb.toString();
        
    }
    
    public static Document xmlFromApi (String uri, String patNum, String[] fields) {
        try {
            StringBuilder uriSb = new StringBuilder(uri);
            StringBuilder querySb = new StringBuilder("{\"patent_number\":");
            querySb.append("\"");
            querySb.append(patNum);
            querySb.append("\"}");
            
            StringBuilder fieldsSb = new StringBuilder("[");
            for (String field : fields) {
                fieldsSb.append("\"");
                fieldsSb.append(field);
                fieldsSb.append("\",");
            }
            if (fieldsSb.charAt(fieldsSb.length() - 1) == ',') {
                fieldsSb.deleteCharAt(fieldsSb.length() - 1);
            }
            fieldsSb.append("]");
            uriSb.append("q=");
            uriSb.append(querySb.toString());
            uriSb.append("&f=");
            uriSb.append(fieldsSb.toString());
            uriSb.append("&format=xml");
            
            URL url = new URL(uriSb.toString());
            HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/xml");

            InputStream xml = connection.getInputStream();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xml);
            return doc;
        }
        catch (Exception e) {
            System.out.println("Error getting data from API: " + e.toString());
            return null;
        }
    
    }
    public static class Doc {
        private String id;
        private HashMap<String, String> entries;
        
        public void removeEntryDuplicates() {
            for (String key : entries.keySet()) {
                if (getEntry(key) == null) {
                    return;
                }
                String[] entries = getEntry(key).split("\\|\\|");
                int end = entries.length;
                Set<String> set = new HashSet<String>();
                for(int i = 0; i < end; i++){
                    set.add(entries[i]);
                }
                Iterator it = set.iterator();
                StringBuilder sb = new StringBuilder();
                while(it.hasNext()) {
                    sb.append(it.next());
                    if (it.hasNext()) {
                        sb.append("||");
                    }
                }
                replaceEntry(key, sb.toString());
                
            }
        }
        public String getId() { return id; }
        public String getEntry(String key) {
            return entries.get(key);
        }
        public Doc() {
            //id = newId;
            entries = new HashMap<String, String>();
        }
        public void replaceEntry(String key, String value) {
            entries.put(key, value);
        }
        public void removeEntry(String key) {
            entries.remove(key);
        }
        public Set<String> getKeys() {
            return entries.keySet();
        }
        public void addEntry(String key, String value) {
            if (entries.containsKey(key)) {
                entries.put(key, entries.get(key) + "||" + value);
            }
            else {
                entries.put(key, value);
            }
        }
        public void joinEntries(String newKeyName, String[] oldCol, String sep, String spacer, boolean deleteOld) {
            String[][] oldItemList = new String[oldCol.length][];
           
          
            for (int i = 0; i < oldItemList.length; ++i) {
                if (getEntry(oldCol[i]) == null) { return; }
                oldItemList[i] = getEntry(oldCol[i]).split(sep);
            }
            int itemCount = oldItemList[0].length;
            for (String[] items : oldItemList) {
                if (items.length != itemCount) {
                    System.out.println("Error merging columns: item count mismatch");
                    return;
                }
                itemCount = items.length;
            }
            String[] newItemList = new String[oldItemList[0].length];
            for (int col = 0; col < oldItemList[0].length; col++) {
                StringBuilder sb = new StringBuilder();
                for (int row = 0; row < oldItemList.length; ++row) {
                    sb.append(oldItemList[row][col]);
                    if (row < oldItemList.length - 1) {
                        sb.append(spacer);
                    }
                }
                newItemList[col] = sb.toString();
            }
            StringBuilder newSb = new StringBuilder();
            for (String str : newItemList) {
                addEntry(newKeyName, str);
            }
            if (deleteOld) {
                for (String col : oldCol) {
                   removeEntry(col); 
                }
                
            }
        }
    }
    
    
    
    /* Output to CSV */
    public static void printCSV(ArrayList<Doc> entries, String filename) throws
    FileNotFoundException {
        PrintWriter out = new PrintWriter(new File(filename));
        StringBuilder sb = new StringBuilder();
        
        ArrayList<String> cols = new ArrayList<String>();
        for (String str: entries.get(5).getKeys()) {
            cols.add(str);
            sb.append(escapeForCSV(str));
            sb.append(",");
        }
        sb.append("\n");
        for (Doc entry : entries) {
            for (String key : cols) {
                
                sb.append(escapeForCSV(entry.getEntry(key)));
                
                sb.append(",");
                
            }
            sb.append("\n");
        }
        out.write(sb.toString());
        out.close();
    }
    public static void printTXT(String keyName, ArrayList<Doc> entries, String filename) throws
    FileNotFoundException {
        PrintWriter out = new PrintWriter(new File(filename));
        StringBuilder sb = new StringBuilder();
        for (Doc entry : entries) {
            sb.append(entry.getEntry(keyName) + "\n");
        }
        out.write(sb.toString());
        out.close();
    }
    /* Return a string that is safe for insertion as field in CSV */
    public static String escapeForCSV(String original) {
        if (original == null) {
            return "";
        }
        original = original.replace("\"","\"\"");
        StringBuilder sb = new StringBuilder(original);
        
        if (original.contains("\"") || original.contains(",") || original.contains("\n")) {
            sb.insert(0, "\"");
            sb.append("\"");
        }
        return sb.toString().trim();
    }
}
