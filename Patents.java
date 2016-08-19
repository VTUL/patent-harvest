/*
 *  
 *  Description: Harvests all USPTO patents with VT affiliated assignees. 
 *  Gets the PDF's too.
 * 
 *  Author: Philippe Gray
 * 
 *  You need to install WGET: `brew install wget`
 *  Also need Javax jar: https://java.net/projects/jsonp/downloads/download/ri/javax.json-ri-1.0.zip
 *  
 *  Compile: javac -cp javax.json-1.0.jar Patents.java
 *  Run: java -cp "javax.json-1.0.jar:." Patents
 *  
 *  Run with: `java Patents`
 *      Program will output PatentMetadata.csv
 *      and download all PDF's [patentNumber].pdf
 *      the directory `filedump`
 * 
 * 
 *  Uses both patentsview.org and USPTO's backend (to get pdfs)
 * 
 * 
 * */

import java.util.*;
import java.text.*;
import javax.xml.*;
import javax.json.*;
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
        
        
        /* CSV options */
        String apiBaseUrl = "http://www.patentsview.org/api/patents/query";
        String selectBy = "assignee_organization";
        String[] selectVals = {"virginia tech", "vpi", "virginia polytechnic"};
        String csvOutputPath = "./VTPatents.csv";
        String[] leadingCols = {"filename"};
        // Can just set as large number you know is greater than # of patents
        int patentCount = 1000;

         String[] desiredFields = {"assignee_organization", "assignee_last_name", "assignee_first_name", "inventor_last_name",
             "inventor_first_name", "app_date", "patent_date", "patent_abstract", "app_number", "patent_number", "uspc_subclass_id",
             "cpc_subgroup_id", "patent_title", "patent_type"};
             
        // File containing list of patent ID's that you want harvested additionally
        // use empty string for none
        String extraPatentIdFile = "./others.txt";
        String idFieldName = "patent_number";
        
        /* PDF Options */
        String usptoPDFPath = "http://pimg-fpiw.uspto.gov/fdd/";
        boolean getPDFs = false;
        String pdfDumpPath = "./filedump";
        String csvPDFFieldName = "dc.identifier.patentnumber";
        
        /*****************************************************/
        
        
        ArrayList<Doc> database = new ArrayList<Doc>();
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
        .newInstance();
        
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        Date dateobj = new Date();
     
        System.out.println("\n[INFO] Downloading XML from " + apiBaseUrl);
        System.out.println("\n[INFO] Selecting by `" + selectBy + "`");
        System.out.println("Where `" + selectBy + "` contains any of the following: ");
        System.out.println(Arrays.toString(selectVals));
        
        Document allPatents = xmlFromApi(apiBaseUrl, selectBy, selectVals, desiredFields, extraPatentIdFile, idFieldName, patentCount);
        
        System.out.println("\n[INFO] Getting fields: ");
        System.out.println(Arrays.toString(desiredFields));
        
   
        Element root = (Element)allPatents.getElementsByTagName("root").item(0);
        Element patents = (Element)root.getElementsByTagName("patents").item(0);
        NodeList entries = patents.getChildNodes();
        
        System.out.println("\n[INFO] Extracting metadata for " + entries.getLength() + " patent(s)");
        System.out.printf("\n[OPTION] Would you like to download PDF's (Y/N): ");
            
        Scanner in = new Scanner(System.in);
        if (in.next().toLowerCase().equals("y")) {
            getPDFs = true;
        } 
        else {
            getPDFs = false;
        }

        for (int i = 0; i < entries.getLength(); ++i) {
            Node entry = entries.item(i);
            
            Doc myDoc = new Doc();
            
            
            
            // Add all text fields using recursive helper
            addChildValuesHelper(myDoc, entry);
            database.add(myDoc);
           
            
            // Doing some processing ***********************************
            
            // dc.contributor.assignee
            String assigneeOrg = myDoc.getEntry("assignee_organization");
            if (assigneeOrg != null && assigneeOrg.length() >= 1) {
                myDoc.addEntry("dc.contributor.assignee", assigneeOrg);
            }
            else {
                myDoc.joinEntries( "dc.contributor.assignee", 
                new String[] {"assignee_last_name", "assignee_first_name"}, "\\|\\|", ", ", false);
            }
            myDoc.removeEntry("assignee_organization");
            myDoc.removeEntry("assignee_last_name");
            myDoc.removeEntry("assignee_first_name");
            
            // dc.creator
            myDoc.joinEntries("dc.contributor.inventor", 
                new String[] {"inventor_last_name", "inventor_first_name"}, "\\|\\|", ", ", true);
                
            // dc.subject.uspc and dc.subject.uspccrossref
            myDoc.splitAtIndex("uspc", "dc.subject.uspc", "dc.subject.uspcother", 1, true);
            
            // dc.date.accessed
            myDoc.addEntry("dc.date.accessed", df.format(dateobj));
            
            // Bulk rename
            myDoc.bulkRename(new String[] {"app_date", "patent_date", "patent_abstract",
                 "app_number", "patent_number", "cpcs", "patent_title", "patent_type"}, 
                new String[] {"dc.date.filed", "dc.date.issued", "dc.description.abstract",
                     "dc.identifier.applicationnumber", "dc.identifier.patentnumber", "dc.subject.cpc",
                     "dc.title", "dc.type.patenttype"}, true);
              
            // Using renamed fields
            // dc.identifier.url
            myDoc.addEntry("dc.identifier.url", usptoPDFPath + getUsptoFilename(myDoc.getEntry(csvPDFFieldName)));
            
            // filename
            myDoc.addEntry("filename", myDoc.getEntry(csvPDFFieldName) + ".pdf");
            
                
        }
        System.out.println("\n[INFO] Exporting `" + csvOutputPath + "`");
        printCSV(database, leadingCols, csvOutputPath);
   
        
        if (getPDFs) {
            // Download PDFs 
            System.out.println("[INFO] Dumping PDFS to `" + pdfDumpPath + "`");
            System.out.println("[INFO] This will take a while!");
            Process p = Runtime.getRuntime().exec("mkdir ./filedump");
            for (Doc doc : database) {
                StringBuilder sb = new StringBuilder("wget  -O " + pdfDumpPath + "/" + doc.getEntry(csvPDFFieldName) + ".pdf");
                // Url is based on pat number
                
                sb.append(" ");
                sb.append(usptoPDFPath);
                
                String baseUrl = sb.toString();
                String patNumName = getUsptoFilename(doc.getEntry(csvPDFFieldName));
                sb.append(patNumName);
                System.out.println("GET " + doc.getEntry(csvPDFFieldName) + ".pdf");
                p = Runtime.getRuntime().exec(sb.toString());
                p.waitFor();
                

            }
        }
         
        
    }
    /* Recursive helper to add all child elements' key, value pairs 
     * to a given doc
     *  */
    public static void addChildValuesHelper(Doc myDoc, Node parent ) {
        NodeList children = parent.getChildNodes();
        if (children.getLength() == 1 && (children.item(0).getChildNodes().getLength() == 1 || children.item(0).getChildNodes().getLength() == 0)) {
           
            myDoc.addEntry(parent.getNodeName(), parent.getTextContent());
            return;
        }
        
        for (int i = 0; i < children.getLength(); ++i) {
            addChildValuesHelper(myDoc, children.item(i));
        }
    }
    
    
    
    
    /* Conversion of patent number to filename  - needed for getting
     * the PDF's
     * */
    public static String getUsptoFilename(String id) {
        if (id == null) {
            return "";
        }
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
  
    
    /* Get a Document containing all patent results
     * 
     * Example of created request
     * */
    public static Document xmlFromApi(String base, String selectBy, String[] selectVals, String[] fields, String extraIdFile, String idFieldName, int entryCount) {
        
        
        StringBuilder requestSb = new StringBuilder(base);
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        
        
        // Get the query part of string
        JsonArrayBuilder selectList = factory.createArrayBuilder();
        for (String val : selectVals) {
            selectList = selectList.add(factory.createObjectBuilder()
                .add("_contains", factory.createObjectBuilder()
                    .add(selectBy, val)));
        }
        
        // Add extra patents from file
        if (extraIdFile.length() > 0) {
            try {
                File file = new File(extraIdFile);
                Scanner fin = new Scanner(file);
                while (fin.hasNextLine()) {
                    selectList = selectList.add(factory.createObjectBuilder()
                        .add("_contains", factory.createObjectBuilder()
                            .add(idFieldName, fin.nextLine().trim())));
                }
            }
            catch (Exception e) {
                // Carry on...
            }
            
        }
        
        String queryPart = Json.createObjectBuilder().
            add("_or", selectList).build().toString();
        
        // Get fields part of string
        String[] newFields = new String[fields.length];
        for (int i = 0; i < newFields.length; ++i) {
            StringBuilder sb = new StringBuilder("\"");
            sb.append(fields[i]);
            sb.append("\"");
            newFields[i] = sb.toString();
        }
        String fieldsPart = Arrays.toString(newFields);
        
        // Get entry count part
        String entryCountPart = factory.createObjectBuilder()
            .add("pages", "1")
            .add("per_page", Integer.toString(entryCount))
            .build().toString();
            
        // Get format part
        
        requestSb.append("?q=");
        requestSb.append(urlEncode(queryPart));
        requestSb.append("&f=");
        requestSb.append(urlEncode(fieldsPart));
        requestSb.append("&o=");
        requestSb.append(urlEncode(entryCountPart));
        requestSb.append("&format=xml");
       
         
        try {
            URL url = new URL(requestSb.toString());
            HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "text/xml");

            InputStream xml = connection.getInputStream();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xml);
            System.out.println("\n[INFO] Request URI: " + requestSb.toString());
            return doc;
        }
        catch(Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }
    
    
    
    
    /* Output to CSV */
    public static void printCSV(ArrayList<Doc> entries, String[] leadingCols, String filename) throws
    FileNotFoundException {
        PrintWriter out = new PrintWriter(new File(filename));
        StringBuilder sb = new StringBuilder();
        
       
        ArrayList<String> cols = new ArrayList<String>();
        for (String colName : leadingCols) {
            cols.add(colName);
            sb.append(escapeForCSV(colName));
            sb.append(",");
        }
        for (String str: entries.get(5).getKeys()) {
            if (!Arrays.asList(leadingCols).contains(str)) {
                cols.add(str);
                sb.append(escapeForCSV(str));
                sb.append(",");
            }
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
    
    /* Make a url string safe for use*/
    public static String urlEncode(String input) {
        StringBuilder resultStr = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (isUnsafe(ch)) {
                resultStr.append('%');
                resultStr.append(toHex(ch / 16));
                resultStr.append(toHex(ch % 16));
            } else {
                resultStr.append(ch);
            }
        }
        return resultStr.toString();
    }

    public static char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    public  static boolean isUnsafe(char ch) {
        if (ch > 128 || ch < 0)
            return true;
        return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
    }

    
    /* Class representing a single document e.g. a single patent */
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
        /* Creates new column by splitting an old column specified index 
         * Specified index is included in newCol2
         * */
        public void splitAtIndex(String oldCol, String newCol1, String newCol2, int index, boolean deleteOld) {
            if(getEntry(oldCol) == null) {
                return;
            }
            String[] items = getEntry(oldCol).split("\\|\\|");
            StringBuilder val1 = new StringBuilder();
            StringBuilder val2 = new StringBuilder();
            
            int i = 0;
            for (; i < items.length; ++i) {
                if (i < index) {
                    val1.append(items[i]);
                    if (i < index - 1) {
                        val1.append("||");
                    }
                }
                else {
                    val2.append(items[i]);
                    if (i < items.length - 1) {
                        val2.append("||");
                    }
                }
            }
            addEntry(newCol1, val1.toString());
            addEntry(newCol2, val2.toString());
             if (deleteOld) {
               removeEntry(oldCol);
            }
            
        }
        
        /* Allows renaming of multiple keys for a document */
        public void bulkRename(String[] oldNames, String[] newNames, boolean deleteOld) {
            if (oldNames.length != newNames.length) {
                System.out.println("Error: Can't perform bulk rename. Length of old and new name list differ");
            }
            for (int i = 0; i < oldNames.length; ++i) {
                entries.put(newNames[i], entries.get(oldNames[i]));
                if (deleteOld) {
                    removeEntry(oldNames[i]);
                }
                
            }
        }
    }
}
