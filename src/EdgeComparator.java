package src;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class EdgeComparator {

    public static LinkedHashMap<Integer, Double> sortMap(HashMap<Integer, Double> map) {

        // Create a list from elements of HashMap
        List<Map.Entry<Integer, Double>> list = new LinkedList<Map.Entry<Integer, Double>>(
            map.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
            public int compare(Map.Entry<Integer, Double> o1,
                    Map.Entry<Integer, Double> o2) {
                return -1 * (new Double(o1.getValue())).compareTo(new Double(o2.getValue()));
            }
        });

        // put data from sorted list to hashmap
        LinkedHashMap<Integer, Double> sortedFront = new LinkedHashMap<Integer, Double>();
        for (Map.Entry<Integer, Double> aa : list) {
            sortedFront.put(aa.getKey(), aa.getValue());
        }
        return sortedFront;
    }
}