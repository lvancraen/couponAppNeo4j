package couponAppBackEnd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Content.CType;

import org.jdom2.DataConversionException;

public class OSM_Data {

	private OSM_Graph graph = new OSM_Graph();
	private HashMap<Long, OSM_Node> nodes;
	private HashSet<OSM_Way> ways;
	private HashSet<OSM_Node> sharedNodes;
	private HashSet<Long> duplicateNodeIDs = new HashSet<Long>();
	
	public OSM_Data() {
		nodes = new HashMap<Long, OSM_Node>();
		ways = new HashSet<OSM_Way>();
	}
	
	public void buildGraph() throws IOException {
		graph.createDB(nodes, ways, sharedNodes);
	}

	public void addNode(OSM_Node node) {
		nodes.put(node.getNodeID(), node);
	}

	public void addWay(OSM_Way way) {
		ways.add(way);
	}
	
	public void clearSingleNodes() {
		
		System.out.println("Removing Extra Nodes...");
		
		HashSet<Long> nodeIDs = new HashSet<Long>();
		
	
		Iterator <OSM_Node> nodeIterator  = nodes.values().iterator();
		//First we create a set with all nodeIDs
		while (nodeIterator.hasNext()) {
			nodeIDs.add(nodeIterator.next().getNodeID());
		}
		
		Iterator <OSM_Way> wayIterator = ways.iterator();
		
		//We remove all nodesIDs that are contained in our set of ways. 
		//The first time we find a nodeID we will be able to remove it
		//But if the node exists in two ways then the second time we try to
		//it will already be gone. So we save that nodeID in the duplicateNodeIDs array
		while (wayIterator.hasNext()) {
			OSM_Way currentWay = wayIterator.next();
			Iterator<Long> nodeIDIterator = currentWay.getNodeIDs().iterator();
			while (nodeIDIterator.hasNext()) {
				Long currentNodeID = nodeIDIterator.next();
				if (nodeIDs.contains(currentNodeID)) {
					nodeIDs.remove(currentNodeID);
				}
				else {
					duplicateNodeIDs.add(currentNodeID);
					
				}
			}
		}
		//now that we have all the duplicate IDs, it's time to store the nodes themselves into a hashMap
		nodeIterator  = nodes.values().iterator();
		sharedNodes = new HashSet <OSM_Node>();
		while (nodeIterator.hasNext()) {
			OSM_Node currentNode = nodeIterator.next();
			if (duplicateNodeIDs.contains(currentNode.getNodeID())) {
				currentNode.setIsIntersection();
				sharedNodes.add(currentNode);
			}
		}
	
		System.out.println("Found " + duplicateNodeIDs.size() + " duplicate nodes" );
		System.out.println("Total ways:" + ways.size());
		System.out.println("Total nodes: " + nodes.size() + ". Final nodes: " + sharedNodes.size());
		
		findImportantNodesOfWays(ways);
		
		System.out.println("New Final nodes size: "+sharedNodes.size());
		
	}
	//finds all nodes that will be populated in the Neo4j graph
	//and add the nodes into the sharedNodes HashSet that aren't already in their
	private void findImportantNodesOfWays(HashSet<OSM_Way> way) {
		System.out.println("Newly added intersection nodes: ");
		Iterator<OSM_Way> wayIterator = way.iterator();
		OSM_Node tempNode;
		while (wayIterator.hasNext()) {
			OSM_Way currentWay = wayIterator.next();
			Iterator<Long> NodeIDIterator = currentWay.getNodeIDs().iterator();
			long currentNodeID = -1;
			boolean firstElement = true;
			while (NodeIDIterator.hasNext()) {
				currentNodeID = NodeIDIterator.next();
				tempNode = nodes.get(currentNodeID);
				if (firstElement) {
					currentWay.addIntersectionNode(currentNodeID);
					if (!sharedNodes.contains(tempNode)) {
						sharedNodes.add(tempNode);
						duplicateNodeIDs.add(currentNodeID);
						System.out.println(currentNodeID);
					}
					firstElement = false;
				} else {
					tempNode = nodes.get(currentNodeID);
					if (tempNode.getIntersection()) {
						currentWay.addIntersectionNode(currentNodeID);
					}
				}
			}
			tempNode = nodes.get(currentNodeID);
			if(tempNode.getIntersection()) {
				continue;
			} else {
				currentWay.addIntersectionNode(currentNodeID);
				sharedNodes.add(tempNode);
				duplicateNodeIDs.add(currentNodeID);
				System.out.println(currentNodeID);
			}
		}
		
	}
	
}
