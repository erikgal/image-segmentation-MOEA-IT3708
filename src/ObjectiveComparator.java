package src;

import java.util.Comparator;
import java.util.Map;

class ObjectiveComparator implements Comparator {
	Map<Integer, double[]> map;
    int sort;
    int objective;
 
	public ObjectiveComparator(Map<Integer, double[]> map, String sort, int objective) {
		this.map = map;
        this.sort = sort == "Ascending" ? -1 :  1;
        this.objective = objective;
	}
    
    // Sorts by overall deviation
	public int compare(Object keyA, Object keyB) {
		Comparable valueA = (Comparable) map.get(keyA)[objective];
		Comparable valueB = (Comparable) map.get(keyB)[objective];
		int result = this.sort * valueB.compareTo(valueA);
		//return result == 0 ? (keyA.toString()).compareTo(keyB.toString()) : result; // allows duplicates
		return result;
	}
}
