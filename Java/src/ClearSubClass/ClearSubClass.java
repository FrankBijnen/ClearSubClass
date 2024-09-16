package ClearSubClass;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class ClearSubClass {

	private static Boolean XMLChanged = false;
	private static String FILENAME = null;
    private static final String DIRECTSUBCLASS = "000000000000FFFFFFFFFFFFFFFFFFFFFFFF";
    
    public static Boolean ProcessSubClass(Element TextElement, String RtePtName) {
    	
    	Boolean result = false;
    
	    Node SubClassNode = TextElement.getElementsByTagName("gpxx:Subclass").item(0);
	    if (SubClassNode != null)
	    {
	  	  String SubClassValue = SubClassNode.getTextContent();
	  	  // Check if it is 'clear'
	  	  if (!SubClassValue.equals(DIRECTSUBCLASS)) {
	  		  SubClassNode.setTextContent(DIRECTSUBCLASS);
	  		  if (!RtePtName.isEmpty()) {
		      	  String SubClassValueAfter = SubClassNode.getTextContent();
		  		  System.out.printf("Route point %s modified, SubClass %s==>%s %n", RtePtName, SubClassValue, SubClassValueAfter);
	  		  }
	  		result = true;
	  	  }
	    }
	    return result;
    }

	public static void main(String[] args) throws Exception {
      if (args.length > 0)
	  {
    	  FILENAME = args[0];
      }
      else
      {
    	  JFileChooser fileChooser = new JFileChooser();
    	  fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
    	  fileChooser.setFileFilter(new FileNameExtensionFilter("GPX Files","gpx"));
    	  int result = fileChooser.showOpenDialog(null);
    	  if (result == JFileChooser.APPROVE_OPTION) {
    		  FILENAME = fileChooser.getSelectedFile().getAbsolutePath();
    	  }
    	  else
    	  {
    		  throw new Exception("Usage Java -jar ClearSubClass.jar \"gpx file\"");
    	  }
      }
	  System.out.printf("Reading XML %s %n", FILENAME);
      
      // Instantiate the Factory
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      
      try {

          // optional, but recommended
          // process XML securely, avoid attacks like XML External Entities (XXE)
          dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

          // parse XML file
          DocumentBuilder db = dbf.newDocumentBuilder();
          Document doc = db.parse(new File(FILENAME));

          // optional, but recommended
          // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
          //doc.getDocumentElement().normalize();

          // Get Route list
          NodeList Rtelist = doc.getElementsByTagName("rte");
          	
          for (int RteCnt = 0; RteCnt < Rtelist.getLength(); RteCnt++) {
              Node RteNode = Rtelist.item(RteCnt);
              if (RteNode.getNodeType() == Node.ELEMENT_NODE) {
            	  // Cast to Element allows forgetElementsByTagName!
                  Element RteElement = (Element) RteNode;
                  // Find name node of Route
                  String RteName = RteElement.getElementsByTagName("name").item(0).getTextContent();
                  System.out.printf("Checking Route %s %n", RteName);
                  
                  // Get Route points in this route. Dont care for Via or Shaping points.
                  NodeList RtePtList = RteElement.getElementsByTagName("rtept");
                  for (int RtePtCnt = 0; RtePtCnt < RtePtList.getLength(); RtePtCnt++) {
                      Node RtePtNode = RtePtList.item(RtePtCnt);
                      if (RtePtNode.getNodeType() == Node.ELEMENT_NODE) {
                    	  // Cast to Element
                          Element RtePtElement = (Element) RtePtNode;
                          String RtePtName = RtePtElement.getElementsByTagName("name").item(0).getTextContent();
                          // Get the gpxx:RoutePointExtension
                          NodeList RtePtExtension =	RtePtElement.getElementsByTagName("gpxx:RoutePointExtension");
                          if (RtePtExtension.getLength() != 0) {
                              Element RtePtExtElement = (Element) RtePtExtension.item(0);
                        	  // Process Subclass for Route points
                              Boolean HasChangedRtePt = ProcessSubClass(RtePtExtElement, RtePtName);
                              XMLChanged = XMLChanged || HasChangedRtePt; 
/*                              
                        	  // Process Subclass for Ghost points
                              NodeList GpxRptList = RtePtElement.getElementsByTagName("gpxx:rpt");
                              for (int GpxRptCnt = 0; GpxRptCnt < GpxRptList.getLength(); GpxRptCnt++) {
                            	  Element GpxRpt = (Element) GpxRptList.item(GpxRptCnt);
                            	  Boolean HasChangedGhostPt = ProcessSubClass(GpxRpt, "");
                                  XMLChanged = XMLChanged || HasChangedGhostPt; 
                              }
*/                              
                          }
                      }
                  }
              }
          
          }
          if (XMLChanged) {
	          
	          // write dom document to a file
	          try 
	          {
				  //doc.setXmlStandalone(true);
		          DOMSource source = new DOMSource(doc);
	        	  
		          FileOutputStream output = new FileOutputStream(FILENAME);
	        	  Writer writer = new OutputStreamWriter(output, "UTF-8");
	        	  writer.write("\ufeff"); // Looks nicer I think
		          StreamResult result = new StreamResult(writer);
		          
		          TransformerFactory transformerFactory = TransformerFactory.newInstance();
		          Transformer transformer = transformerFactory.newTransformer();
		          transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		          transformer.transform(source, result);
	        	  
	    		  System.out.printf("XML %s saved. %n", FILENAME);
	          } catch ( Exception e) {
	        	  e.printStackTrace();
	          }
          }
          else {
    		  System.out.printf("XML %s not modified. All Subclasses where already cleared.%n", FILENAME);
          }
       } catch (ParserConfigurationException | SAXException | IOException e) {
    	   e.printStackTrace();
       }
	}
}
