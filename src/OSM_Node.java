package couponAppBackEnd;

import java.util.List;

public class OSM_Node {

	private  double lat;
	private  double lon;
	private  long nodeID;
	private boolean isIntersection;
	private String intersectionName;
	private long wayNeo4jID;
	private long wayID;
	private List<String> category;
	
	public OSM_Node(long id, double latitude, double longitude, boolean inter) {
		nodeID = id;
		lat = latitude;
		lon = longitude;
		isIntersection = inter;
	}
	
	public String toString() {
		return "NodeID: " + nodeID + "\tLatitude: " + lat + "\tLongitude: " + lon;
	}
	
	public long getNodeID() {
		return nodeID;
	}
	
	public void setNodeID(long id) {
		nodeID = id;
	}
	
	public void setWayNeo4jID(long id) {
		wayNeo4jID = id;
	}
	
	public long getWayID() {
		return wayID;
	}
	
	public void setWayID(long id) {
		wayID = id;
	}
	
	public long getWayNeo4jID() {
		return wayNeo4jID;
	}
	
	public double getLat() {
		return lat;
	}
	
	public void setLat(double latitude) {
		lat = latitude;
	}
	
	public double getLon() {
		return lon;
	}
	
	public void setLon(double longitude) {
		lon = longitude;
	}
	
	public boolean getIntersection() {
		return isIntersection;
	}
	
	public void setIsIntersection() {
		isIntersection = true;
	}
	
	public String getName() {
		return intersectionName;
	}
	
	public void setName(String name) {
		intersectionName = name;
	}
	
	public List<String> getCategories() {
		if (category.size() == 0) {
			return null;
		} else {
		return category;
		}
	}
	
	public void setCategory(String name) {
		category.add(name);
	}
	
}
