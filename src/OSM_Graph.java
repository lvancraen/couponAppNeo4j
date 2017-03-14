package couponAppBackEnd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.io.fs.FileUtils;

public class OSM_Graph {
	//change path to wherever you want Neo4j database to be built
	private static final String DB_PATH = "/Users/lvancraen/Documents/Summer Research Project 2016/Neo4j-Graph1";
	GraphDatabaseService db;
	OSM_Queries query;
	
	private static enum nodeLabel implements Label {
		SHOP, LOCATION, INTERSECTION, INNER_NODE;
	}
	
	private static enum edgeLabel implements RelationshipType {
		STREET, STREET_SEGMENT, HAS;
	}
	
	public void createDB(HashMap<Long, OSM_Node> nodes, HashSet<OSM_Way> ways, HashSet<OSM_Node> sharedNodes) throws IOException {
		
		FileUtils.deleteRecursively(new File(DB_PATH));
		
		db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
		registerShutdownHook(db);
		
		try (Transaction tx = db.beginTx()) {
			
			Schema schema = db.schema();
			IndexDefinition indexLat = schema.indexFor(Label.label("INTERSECTION")).on("Latitude").create();
			IndexDefinition indexLon = schema.indexFor(Label.label("INTERSECTION")).on("Longitude").create();
			System.out.println("Index on Longitude and Latitude was created");
			tx.success();
			
		}
		
		try (Transaction tx = db.beginTx()) {
			
			Iterator <OSM_Node> sharedNodeIterator  = sharedNodes.iterator();
			Iterator <OSM_Way> wayIterator = ways.iterator();
			Iterator <Long> wayNodeIterator;
			int wayCount = 0;
			int nodeCount = 0;
			
			//creates all intersection nodes in the graph
			while (sharedNodeIterator.hasNext()) {
				OSM_Node currentNode = sharedNodeIterator.next();
				Node tempNode = db.createNode(nodeLabel.INTERSECTION);
				tempNode.setProperty("nodeID", currentNode.getNodeID());
				tempNode.setProperty("Longitude", currentNode.getLon());
				tempNode.setProperty("Latitude", currentNode.getLat());
				tempNode.setProperty("isIntersection", currentNode.getIntersection());
				//nodeCount++;
				//System.out.println("Node count " + nodeCount);
			}
			
			//creates all edges in the graph
			while (wayIterator.hasNext()) {
				OSM_Way currentWay = wayIterator.next();
				wayNodeIterator = currentWay.getIntersectionNodes().iterator();
				long startNodeID = -1;
				long endNodeID = -1;
				Node startNode;
				Node endNode;
				
				while (wayNodeIterator.hasNext()) {
					if (startNodeID == -1) {
						startNodeID = wayNodeIterator.next();
						startNode = db.findNode(nodeLabel.INTERSECTION, "nodeID", startNodeID);
					} else {
						startNodeID = endNodeID;
						startNode = db.findNode(nodeLabel.INTERSECTION, "nodeID", startNodeID);
					}
					if (wayNodeIterator.hasNext()) {
						endNodeID = wayNodeIterator.next();
						endNode = db.findNode(nodeLabel.INTERSECTION, "nodeID", endNodeID);
					} else {
						break;
					}
					long distance = findDistance(startNode, endNode);
					Relationship relationship = startNode.createRelationshipTo(endNode, edgeLabel.STREET_SEGMENT);
					wayCount++;
					relationship.setProperty("wayID", currentWay.getWayID());
					relationship.setProperty("wayType", currentWay.getWayType());
					relationship.setProperty("wayName", currentWay.getWayName());
					relationship.setProperty("distance", distance);
					long startID = (long) startNode.getProperty("nodeID");
					int startNodeIndex = currentWay.getNodeIDs().indexOf((long)startNode.getProperty("nodeID"));
					int endNodeIndex = currentWay.getNodeIDs().indexOf((long)endNode.getProperty("nodeID"));
					List<Long> tempArray = new ArrayList<Long>();
					if (endNodeIndex-startNodeIndex <= 1) {
						relationship.setProperty("nodeIDs", tempArray.toArray(new Long[tempArray.size()]));
					} else {
						tempArray = currentWay.getNodeIDs().subList(startNodeIndex+1, endNodeIndex);
						relationship.setProperty("nodeIDs", tempArray.toArray(new Long[tempArray.size()]));
						//System.out.println("way count " + wayCount);
					}
					List<Double> latArray = new ArrayList<Double>();
					List<Double> lonArray = new ArrayList<Double>();
					for (long id : tempArray) {
						OSM_Node tempNode = nodes.get(id);
						latArray.add(tempNode.getLat());
						lonArray.add(tempNode.getLon());
					}
					relationship.setProperty("Latitudes", latArray.toArray(new Double[latArray.size()]));
					relationship.setProperty("Longitudes", lonArray.toArray(new Double[lonArray.size()]));
				}
			}
			
			System.out.println("New Final way count: "+wayCount);
			tx.success();
		}
		
		System.out.println("Done Succesfully");
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
	
	private long findDistance(Node startNode, Node endNode) {
		
		int R = 6371000;
			
		//tempNode is a place holder for keeping track of furthest node
		long tempNode[] = {-1,-1};
		double startLat = (double)startNode.getProperty("Latitude");
		double phi1 = Math.toRadians(startLat);
		double startLon = (double) startNode.getProperty("Longitude");
	
		double phi2 = Math.toRadians((double) endNode.getProperty("Latitude"));
		double currentLon = (double) endNode.getProperty("Longitude");
		double deltaLambda = Math.toRadians(currentLon - startLon);
		double deltaPhi = Math.toRadians((double) endNode.getProperty("Latitude") - startLat);
		double distance = Math.acos(Math.sin(phi1)*Math.sin(phi2) + Math.cos(phi1)*Math.cos(phi2)*Math.cos(deltaLambda))*R;
		//System.out.println("Distance of: "+currentNode.getNodeID()+", "+distance);
			
		return (long) distance;
	}
}
