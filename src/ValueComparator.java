package src;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class ValueComparator {

	public static HashMap<Integer, Double> sortMap(HashMap<Integer, Double> map, Integer order) {
		// Create a list from elements of HashMap
		List<Map.Entry<Integer, Double>> list = new LinkedList<Map.Entry<Integer, Double>>(
			map.entrySet());

		// Sort the list
		Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
			public int compare(Map.Entry<Integer, Double> o1,
					Map.Entry<Integer, Double> o2) {
				return order * (o1.getValue()).compareTo(o2.getValue());
			}
		});

		// put data from sorted list to hashmap
		HashMap<Integer, Double> sortedFront = new LinkedHashMap<Integer, Double>();
		for (Map.Entry<Integer, Double> aa : list) {
			sortedFront.put(aa.getKey(), aa.getValue());
		}
		return sortedFront;
	}

}
