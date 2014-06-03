package com.eolwral.osmonitor.networks;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;



public class WhoisSAX extends DefaultHandler{

     // ===========================================================
     // Fields
     // ===========================================================
     
     private boolean in_ip = false;
     private boolean in_country = false;
     private boolean in_region = false;
     private boolean in_isp = false;
     private boolean in_org = false;
     private boolean in_latitude = false;
     private boolean in_longitude = false;
     
     private WhoisSAXDataSet ParsedDataSet =  new WhoisSAXDataSet();

     // ===========================================================
     // Getter & Setter
     // ===========================================================

     public WhoisSAXDataSet getParsedData() {
         return this.ParsedDataSet;
     }

     // ===========================================================
     // Methods
     // ===========================================================
     @Override
     public void startDocument() throws SAXException {
    	 Log.w("Debug", "StartDocument");
     }

     @Override
     public void endDocument() throws SAXException {
    	 Log.w("Debug", "EndDocument");
          // Nothing to do
     }

     /** Gets be called on opening tags like:
      * <tag>
      * Can provide attribute(s), when xml was like:
      * <tag attribute="attributeValue">*/
     @Override
     public void startElement(String namespaceURI, String localName,
               String qName, Attributes atts) throws SAXException {
    	 
    	 if (localName.equals("ip")) 
    		 this.in_ip = true;
    	 else if (localName.equals("countrycode")) 
    		 this.in_country = true;
    	 else if (localName.equals("region")) 
    		 this.in_region = true;
    	 else if (localName.equals("isp")) 
    		 this.in_isp = true;
    	 else if (localName.equals("org")) 
    		 this.in_org = true;
    	 else if (localName.equals("latitude")) 
    		 this.in_latitude = true;
    	 else if (localName.equals("longitude")) 
    		 this.in_longitude = true;
     }
     
     /** Gets be called on closing tags like:
      * </tag> */
     @Override
     public void endElement(String namespaceURI, String localName, String qName)
               throws SAXException {
    	 if (localName.equals("ip")) 
    		 this.in_ip = false;
    	 else if (localName.equals("countrycode")) 
    		 this.in_country = false;
    	 else if (localName.equals("region")) 
    		 this.in_region = false;
    	 else if (localName.equals("isp")) 
    		 this.in_isp = false;
    	 else if (localName.equals("org")) 
    		 this.in_org = false;
    	 else if (localName.equals("latitude")) 
    		 this.in_latitude = false;
    	 else if (localName.equals("longitude")) 
    		 this.in_longitude = false;
     }
     
     /** Gets be called on the following structure:
      * <tag>characters</tag> */
     @Override
    public void characters(char ch[], int start, int length) {
        if(this.in_ip)
        	ParsedDataSet.setip(new String(ch, start, length));
        else if(this.in_country)
        	ParsedDataSet.setcountry(new String(ch, start, length));
        else if(this.in_region)
        	ParsedDataSet.setregion(new String(ch, start, length));
        else if(this.in_isp)
        	ParsedDataSet.setisp(new String(ch, start, length));
        else if(this.in_org)
        	ParsedDataSet.setorg(new String(ch, start, length));
        else if(this.in_latitude)
        	ParsedDataSet.setlatitude(new String(ch, start, length));
        else if(this.in_longitude)
        	ParsedDataSet.setlongitude(new String(ch, start, length));
    }
}