package couponAppBackEnd;

import java.util.ArrayList;
import java.util.List;

public class queryTester {

	public static void main(String[] args) {
        OSM_Queries query = new OSM_Queries();
        /*
         * 
         * createNewShop(double latitude, double longitude, long shopID, String shopName)
         * addCategory(List<String> category, long shopID)
         * deleteCategory(List<String> category, long shopID)
         * findShops(List<String> category, double distance, double latitude, double longitude)
         * 		-returns a List of shopIDs which contain the categories wanted
         * 
         */
        query.createNewShop(45.504, -73.58, 2, "Starbucks");
        //List<String> category = new ArrayList<String>();
        //category.add("Coffee");
        //category.add("Tea");
        
        //query.addCategory(category, 1);
        //query.findShops(category, 150, 45.5028643, -73.5728833);
        //query.deleteCategory(category, 1);
	}
}
