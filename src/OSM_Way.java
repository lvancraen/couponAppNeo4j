package couponAppBackEnd;

import java.util.ArrayList;


public class OSM_Way {

	private ArrayList <Long> nodeIDs;
	private String type;
	private String name;
	private long wayID;
	private double distance;
	private ArrayList<Long> intersectionIDs = new ArrayList<Long>();
	
	public OSM_Way(long id, String name, String type, ArrayList<Long> nodeIDs) {
		this.wayID = id;
		this.type = type;
		this.name = name;
		this.nodeIDs = new ArrayList (nodeIDs);
	}
	
	public String toString() {
		return "WayID: " + wayID + "\tName: " + name + "\tType: " + type + "\tNode IDs:" + nodeIDs;

	}
	
	public ArrayList<Long> getNodeIDs() {
		return nodeIDs;
	}
	
	public void removeNode(long nodeID) {
		nodeIDs.remove(nodeID);
	}
	
	public long getWayID() {
		return wayID;
	}
	
	public String getWayName() {
		return name;
	}
	
	public String getWayType() {
		return type;
	}
	
	public ArrayList<Long> getIntersectionNodes() {
		return intersectionIDs;
	}
	
	public void addIntersectionNode(long ID) {
		intersectionIDs.add(ID);
	}
	
}
