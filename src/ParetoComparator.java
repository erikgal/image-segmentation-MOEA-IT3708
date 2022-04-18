package src;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class ParetoComparator {

	public static LinkedHashMap<Integer, double[]> sortMap(HashMap<Integer, double[]> map, String sort, int objective) {

        int direction = sort == "Ascending" ? -1 :  1;

		// Create a list from elements of HashMap
		List<Map.Entry<Integer, double[]>> list = new LinkedList<Map.Entry<Integer, double[]>>(
			map.entrySet());

		// Sort the list
		Collections.sort(list, new Comparator<Map.Entry<Integer, double[]>>() {
			public int compare(Map.Entry<Integer, double[]> o1,
					Map.Entry<Integer, double[]> o2) {
				return direction * (new Double(o1.getValue()[objective])).compareTo(new Double(o2.getValue()[objective]));
			}
		});

		// put data from sorted list to hashmap
		LinkedHashMap<Integer, double[]> sortedFront = new LinkedHashMap<Integer, double[]>();
		for (Map.Entry<Integer, double[]> aa : list) {
			sortedFront.put(aa.getKey(), aa.getValue());
		}
		return sortedFront;
	}

}
