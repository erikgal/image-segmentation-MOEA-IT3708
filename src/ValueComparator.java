package src;

import java.util.Comparator;
import java.util.Map;

class ValueComparator implements Comparator {
	Map<Integer, Double> map;
    int sort;
 
	public ValueComparator(Map<Integer, Double> map, String sort) {
		this.map = map;
        this.sort = sort == "Ascending" ? -1 :  1;
	}
    
    // Sorts by overall deviation
	@Override
	public int compare(Object keyA, Object keyB) {
		Comparable valueA = (Comparable) map.get(keyA);
		Comparable valueB = (Comparable) map.get(keyB);
		int result = this.sort * valueB.compareTo(valueA);
		//return result == 0 ? 1 : result; // allows duplicates
		return result;
	}
}
