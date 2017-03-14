package couponAppBackEnd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.consistency.checking.cache.CacheSlots.NodeLabel;
import org.neo4j.cypher.internal.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.neo4j.helpers.collection.IteratorUtil;

public class OSM_Queries {
	
	private static final String DB_PATH = "/Users/lvancraen/Documents/Summer Research Project 2016/Neo4j-Graph1";
	GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File( DB_PATH ));
	private static final double DISTANCE = 1000;
	private static final double ONSITE = 0;
	private String rows = "";
	
	//used to obtain a range of nodes which can then be filtered by category search criteria
	//@param lat is the latitude point
	//@param lon is the longitude point
	//@param distance is the range in meters in which a person wants to filter the graph by
	//@returns a list of OSM_Node's
	private List<OSM_Node> findNearestNode(double lat, double lon, double distance) {
		//System.out.println("Finding range of nodes...");
		registerShutdownHook(db);
		
		double minLat = lat - (distance / 6378000) * (180/Math.PI);
		double maxLat = lat + (distance / 6378000) * (180/Math.PI);
		double minLon = lon - (distance / 6378000) * (180/Math.PI) / Math.cos(lat * (Math.PI/180));
		double maxLon = lon + (distance / 6378000) * (180/Math.PI) / Math.cos(lat * (Math.PI/180));
		
		List<OSM_Node> nodeArray = new ArrayList<OSM_Node>();
		
		try (Transaction ignored = db.beginTx();) {
			Result result = db.execute("MATCH (n)-[s:STREET_SEGMENT]-(m) "
										+ "WHERE n.Latitude>="+minLat
										+" AND n.Latitude<="+maxLat
										+" AND n.Longitude>="+minLon
										+" AND n.Longitude<="+maxLon
										//+" AND s.wayName='"+wayName
										+" WITH DISTINCT s "
										+ "WITH id(s) as wayNeo4jID, s.wayID as wayID, s.nodeIDs AS ids, s.Latitudes AS lats, s.Longitudes AS lons "
										+ "WITH wayNeo4jID, wayID, ids,lats,lons,range(0,size(ids)-1,1) AS col_size "
										+ "WHERE size(ids)=size(lats) AND size(lats)=size(lons) "
										+ "unwind col_size as idx "
										+ "return wayNeo4jID, wayID, ids[idx],lats[idx],lons[idx]");
			
			
			
			
			while ( result.hasNext() ) {
				OSM_Node tempNode = new OSM_Node(-1,-1,-1,false);
				Map<String,Object> row = result.next();
			    for ( Entry<String,Object> column : row.entrySet() ) {
			    	rows += column.getKey()+": "+column.getValue()+"\t";
			    	if (column.getKey().equalsIgnoreCase("ids[idx]")) {
			    		tempNode.setNodeID((long) column.getValue());
			    	}
			    	if (column.getKey().equalsIgnoreCase("lats[idx]")) {
			    		tempNode.setLat((double) column.getValue());
			    	}
			    	if (column.getKey().equalsIgnoreCase("lons[idx]")) {
			    		tempNode.setLon((double) column.getValue());
			    	}
			    	if (column.getKey().equalsIgnoreCase("wayNeo4jID")) {
			    		tempNode.setWayNeo4jID((long) column.getValue());
			    	}
			    	if (column.getKey().equalsIgnoreCase("wayID")) {
			    		tempNode.setWayID((long) column.getValue());
			    	}
			    }
			    nodeArray.add(tempNode);
			    rows += "\n";
			}
			
			Result result2 = db.execute("MATCH (n:LOCATION)-[s:STREET_SEGMENT]-(m) "
					+ "WHERE n.Latitude>="+minLat
					+" AND n.Latitude<="+maxLat
					+" AND n.Longitude>="+minLon
					+" AND n.Longitude<="+maxLon
					+" WITH n"
					+" RETURN DISTINCT n");
			
			OSM_Node tempNode = new OSM_Node(-1,-1,-1,false);
			
			Iterator<Node> columns = result2.columnAs("n");
			while(columns.hasNext()) {
				Node n = columns.next();
				for (String key : n.getPropertyKeys()) {
					if (key.equalsIgnoreCase("nodeID")) {
						tempNode.setNodeID((long) n.getProperty(key));
					}
					if (key.equalsIgnoreCase("Latitude")) {
						tempNode.setLat((double) n.getProperty(key));
					}
					if (key.equalsIgnoreCase("Longitude")) {
						tempNode.setLon((double) n.getProperty(key));
					}
				}
				rows += tempNode;
			    nodeArray.add(tempNode);
			    rows += "\n";
			}
			//System.out.println(rows);
			return nodeArray;
		}
	}
	
	//returns a list of nodeIDs which were in a certain range and have a given category
	public List<Long> findShops(List<String> category, double distance, double lat, double lon) {
		
		List<Long> nodes = new ArrayList<Long>();
		List<OSM_Node> nodeArray = findNearestNode(lat, lon, distance);
		
		Transaction tx = db.beginTx();
		try {
			for (OSM_Node node : nodeArray) {
				if (db.findNode(Label.label("LOCATION"), "nodeID", node.getNodeID()) != null) {
					
					String coupons = "";
					for (String key : category) {
						if (coupons == "") {
							coupons += "['"+key;
						} else {
							coupons += "','"+key;
						}
					}
					coupons += "']";
					Result results = db.execute("MATCH (n:LOCATION)-[:HAS]-(m:SHOP)"
											+ " WHERE n.nodeID="+node.getNodeID()
											+ " WITH m"
											+ " WHERE ALL(x IN "+coupons+" WHERE x IN m.category)"
											+ " RETURN m");
					
					
					Iterator<Node> columns = results.columnAs("m");
					while(columns.hasNext()) {
						Node n = columns.next();
						nodes.add((Long) n.getProperty("nodeID"));
					}
				}
			}
			tx.success();
			System.out.println("The following shopIDs have "+category+" coupons: "+nodes);
			return nodes;
		} finally {
			tx.close();
		}
	}
	
	//add categories (aka coupons) to specific shops
	public void addCategory(List<String> category, long nodeID) {
		
		Transaction tx = db.beginTx();
		try {
			Node n = db.findNode(Label.label("SHOP"), "nodeID", nodeID);
			if (n.hasProperty("category")) {
				String[] coupons = (String[]) n.getProperty("category");
				int size = coupons.length;
				coupons = Arrays.copyOf(coupons, coupons.length + category.size());
				for (String element : category) {
					coupons[size] = element;
					size++;
				}
				n.setProperty("category", coupons);
			} else {
				String[] coupons = category.toArray(new String[category.size()]);
				n.setProperty("category", coupons);
			}
			tx.success();
			System.out.println("Successfully added category to shop");
		} finally {
			tx.close();
		}
	}
	
	public void deleteCategory(List<String> category, long nodeID) {
		
		Transaction tx = db.beginTx();
		try {
			Node n = db.findNode(Label.label("SHOP"), "nodeID", nodeID);
			for (String key : category) {
				db.execute("MATCH (n:SHOP {nodeID:"+nodeID+"})"
						+ " WHERE EXISTS(n.category)"
						+ " SET n.category = FILTER(x IN n.category WHERE x <> '"+key+"')");
			}
			tx.success();
		} finally {
			tx.close();
		}
	}
	
	//checks to see if the node given already exists in the graph as a Location node
	//if yes, only a Shop node is created
	//if no, both a Shop node and a Location node are created
	//@param lat is the latitude point
	//@param lon is the longitude point
	//@param wayName is the name of the street which the new shop will be located on
	//@param id is the identification number given at the time of registration on the app
	//@param shopName is the name of the store
	public void createNewShop(double lat, double lon, long id, String shopName) {
		
		List<OSM_Node> nodeArray = findNearestNode(lat, lon, DISTANCE);
		OSM_Node tempNode = closestTo(nodeArray, lat,lon);
		
		System.out.println("Nearest node: "+tempNode);
		Transaction tx = db.beginTx();
		try {
			if (db.findNode(Label.label("LOCATION"), "nodeID", tempNode.getNodeID()) != null) {
				System.out.println("The node is a Location node. Shop will be attached to this node");
				createShop(tempNode, id, shopName);
			} else {
				System.out.println("Node isn't labeled as a Location node. Therefore must create a new Location node");
				createLocation(tempNode, id, shopName);
				System.out.println("Successsfully created location");
				createShop(tempNode, id, shopName);
				System.out.println("Successsfully created shop");
			}
			tx.success();
		} finally {
			tx.close();
		}
	}
	
	//creates new location node on a edge. Must split this edge in two
	//@param tempNode is the node found in the Neo4j database with a longitude/latitude nearest to the given lon/lat coordinates
	//@param id is the identification number given at the time of registration on the app
	//@param name is the name of the store
	private void createLocation(OSM_Node tempNode, long id, String name) {
		
		System.out.println("Creating new Location node...");
		Transaction tx = db.beginTx();
		try {
			
			db.execute("CREATE (n:LOCATION {nodeID:"+tempNode.getNodeID()
						+",Longitude:"+tempNode.getLon()+",Latitude:"+tempNode.getLat()+",isIntersection:"+tempNode.getIntersection()+"})");
			
			if (db.findNode(Label.label("LOCATION"), "nodeID", tempNode.getNodeID()) != null) {
				System.out.println("Node was created");	
			} else {
				System.out.println("Node was not created");
			}
			
			System.out.println("Now must attach it to existing nodes...");
			
			//Gets index of the Location's nodeID in the way nodeIDs array
			//will be used to set the nodeIDs property of the new relationships
			Relationship relationship = db.getRelationshipById(tempNode.getWayNeo4jID());
			long[] result = (long[]) relationship.getProperty("nodeIDs");
			int index = 0;
			for (long value : result) {
				if (value != tempNode.getNodeID()) {
					index++;
				} else {
					break;
				}
			}
		
			//Creates the relationships between the newly create Location node and the previous two nodes
			db.execute("MATCH (n)-[s:STREET_SEGMENT]-(m),(p:LOCATION {nodeID:"+tempNode.getNodeID()+"})"
					+ " WHERE id(s)="+tempNode.getWayNeo4jID()
					+ " WITH s,n,m,p"
					+ " WHERE startNode(s)=n"
					+ " WITH s,n,m,p"
					+ " CREATE (n)-[r1:STREET_SEGMENT]->(p),(p)-[r2:STREET_SEGMENT]->(m)"
					+ " SET r1=s,r2=s");
			
			
			System.out.println("Edges have been created!");
			
			//deletes old relationship
			db.execute("MATCH (n)-[s:STREET_SEGMENT]-(m)"
					+ " WHERE id(s)="+tempNode.getWayNeo4jID()
					+ " WITH distinct s"
					+ " DELETE s");		
			
			Result results = db.execute("MATCH (n)-[s1:STREET_SEGMENT]-(m)-[s2:STREET_SEGMENT]-(l)"
											+ " WHERE s1.wayID="+tempNode.getWayID()+" AND s2.wayID="+tempNode.getWayID()
											+ " AND m.nodeID="+tempNode.getNodeID()
											+ " WITH startNode(s1) AS a, endNode(s1) AS b, endNode(s2) as c"
											+ " MATCH (a)-[s1:STREET_SEGMENT]-(b)-[s2:STREET_SEGMENT]-(c)"
											+ " RETURN a,c");
			
			double distance1 = -1;
			double distance2 = -1;
			Node n1 = null;
			Node n2 = null;
			
			while (results.hasNext()) {
				Map<String, Object> columns = results.next();
				n1 = (Node) columns.get("a");
				n2 = (Node) columns.get("c");
			}
			distance1 = findDistance(tempNode, (double) n1.getProperty("Latitude"), (double) n1.getProperty("Longitude"));
			distance2 = findDistance(tempNode, (double) n2.getProperty("Latitude"), (double) n2.getProperty("Longitude"));
			
			//set properties of newly constructed relationships
			db.execute("MATCH (n)-[s1:STREET_SEGMENT]-(m)-[s2:STREET_SEGMENT]-(l)"
					+ " WHERE s1.wayID="+tempNode.getWayID()+" AND s2.wayID="+tempNode.getWayID()
					+ " AND ANY(x IN s1.nodeIDs WHERE x = "+tempNode.getNodeID()+")"
					+ " AND ANY(x IN s2.nodeIDs WHERE x = "+tempNode.getNodeID()+")"
					+ " AND startNode(s1)=n AND startNode(s2)=m"
					+ " SET s1.nodeIDs=s1.nodeIDs[.."+index+"], s2.nodeIDs=s2.nodeIDs["+(index+1)+"..]"
					+ " SET s1.Longitudes=s1.Longitudes[.."+index+"], s2.Longitudes=s2.Longitudes["+(index+1)+"..]"
					+ " SET s1.Latitudes=s1.Latitudes[.."+index+"], s2.Latitudes=s2.Latitudes["+(index+1)+"..]"
					+ " SET s1.distance="+distance1+", s2.distance="+distance2);
			
			System.out.println("Edges have been updated with new properties");
			tx.success();
			//createShop(node, id, name);
		} finally {
			tx.close();
		}
	}
	
	
	//creates new Shop node
	//@param tempNode is the node found in the Neo4j database with a longitude/latitude nearest to the given lon/lat coordinates
	//@param id is the identification number given at the time of registration on the app
	//@param name is the name of the store
	private void createShop(OSM_Node tempNode, long id, String name) {
		
		Transaction tx = db.beginTx();
		try {
			Node node = db.createNode(Label.label("SHOP"));
			node.setProperty("nodeID", id);
			node.setProperty("Name", name);
			
			Node locationNode = db.findNode(Label.label("LOCATION"), "nodeID", tempNode.getNodeID());
			Relationship relationship = locationNode.createRelationshipTo(node, RelationshipType.withName("HAS"));
			relationship.setProperty("distance", ONSITE);
			
			tx.success();
		} finally {
			tx.close();
		}
	}
	
	//given a list of nodes and lon/lat coord, returns a node closest to the given lon/lat coordinate
	private OSM_Node closestTo(List<OSM_Node> nodeArray, double lat, double lon) {
		System.out.println("Finding node clostest to given lat/lon coordinates: ");

		double distanceTo = 999999999;
		OSM_Node returnNode = new OSM_Node(-1,-1,-1,false);
		
		for (OSM_Node n : nodeArray) {
			double tempDistance = findDistance(n, lat, lon);
			if (distanceTo > tempDistance) {
				distanceTo = tempDistance;
				returnNode = n;
			}
		}
		
		return returnNode;
	}
	
	//given two nodes and their coordinates, returns the distance between them
	private double findDistance(OSM_Node startNode, double endLat, double endLon) {
			
		int R = 6371000;
				
		double startLat = startNode.getLat();
		double phi1 = Math.toRadians(startLat);
		double startLon = startNode.getLon();
	
		double phi2 = Math.toRadians(endLat);
		double deltaLambda = Math.toRadians(endLon - startLon);
		double distance = Math.acos(Math.sin(phi1)*Math.sin(phi2) + Math.cos(phi1)*Math.cos(phi2)*Math.cos(deltaLambda))*R;
			
		return distance;
	}
	
	public void shutDown() {
		db.shutdown();
	}
	private static void registerShutdownHook(final GraphDatabaseService db) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				db.shutdown();
			}
		});
	}
}
