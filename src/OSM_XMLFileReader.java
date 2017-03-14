package couponAppBackEnd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OSM_XMLFileReader {
	
	public static void main(String[] args) {
		try {
			File inputFile = new File("mcgill.map");
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
	        UserHandler userhandler = new UserHandler();
	        saxParser.parse(inputFile, userhandler);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class UserHandler extends DefaultHandler {
	
	private OSM_Data osmData = new OSM_Data();
	private OSM_Way osmWay;
	
	String currentElement = "";
	
	long nodeID;
	double lat;
	double lon;
	
	long wayID;
	String wayType;
	String wayName;
	boolean isStreet = false;
	ArrayList<Long> tempNodeIDs;
	
	public void endDocument() {
		osmData.clearSingleNodes();
		try {
			osmData.buildGraph();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("node")) {
			currentElement = "node";
			nodeID = Long.parseLong(attributes.getValue("id"));
			lat = Double.parseDouble(attributes.getValue("lat"));
			lon = Double.parseDouble(attributes.getValue("lon"));
			
		} else if (qName.equalsIgnoreCase("way")) {
			currentElement = "way";
			wayID = Long.parseLong(attributes.getValue("id"));
			tempNodeIDs = new ArrayList<Long>();
			
		} else if (qName.equalsIgnoreCase("nd")) {
			if (currentElement == "way") {
				tempNodeIDs.add(Long.parseLong(attributes.getValue("ref")));
			}
			
		} else if (qName.equalsIgnoreCase("tag")) {
			if (currentElement == "way") {
				if (attributes.getValue("k").equalsIgnoreCase("highway")) {
					wayType = attributes.getValue("v");
					isStreet = true;
				} else if (attributes.getValue("k").equalsIgnoreCase("name")) {
					wayName = attributes.getValue("v");
				}
			}
		}
	}
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("node")) {
			OSM_Node newNode = new OSM_Node(nodeID, lat, lon, false);
			osmData.addNode(newNode);
			nodeID = 0;
			lat = 0;
			lon = 0;
			currentElement = "";
		} else if (qName.equalsIgnoreCase("way")) {
			if (isStreet) {
				OSM_Way newWay = new OSM_Way(wayID, wayName, wayType, tempNodeIDs);
				if (wayName.length() > 1) {
					osmData.addWay(newWay);
					System.out.println(newWay);
				}
			}
			isStreet = false;
			wayID = 0;
			wayName = "";
			wayType = "";
			currentElement = "";
		}
	}
	
}
