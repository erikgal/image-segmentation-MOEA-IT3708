package src;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;


public class Fitness {

    public static HashMap<Integer, Double> calculateCrowdingDistance(ArrayList<Map<Integer, double[]>> paretoFronts){
        HashMap<Integer, Double> crowdingDistance = new HashMap<Integer, Double>();
        for (int front = 0; front < paretoFronts.size(); front++) {
            for (int M = 0; M < 3; M++) {
                HashMap<Integer, double[]> paretoFront = (HashMap<Integer, double[]>) paretoFronts.get(front);
                // TreeMap<Integer, double[]> sortedFront = new TreeMap<Integer, double[]>(new ObjectiveComparator(paretoFront, "Descending", M));
                // sortedFront.putAll(paretoFront);
                LinkedHashMap<Integer, double[]> sortedFront = ParetoComparator.sortMap(paretoFront, "Descending", M);

                Iterator<Entry<Integer, double[]>> iterator = sortedFront.entrySet().iterator();
                Integer firstKey = iterator.next().getKey();
                Integer lastKey = firstKey;
                while (iterator.hasNext()) { lastKey = iterator.next().getKey(); }

                crowdingDistance.put(firstKey, Double.MAX_VALUE); // TODO Double check this!
                crowdingDistance.put(lastKey, Double.MAX_VALUE);  // TODO Double check this!
                double fMax = sortedFront.get(firstKey)[M];
                double fMin = sortedFront.get(lastKey)[M];

                Iterator<Integer> keyIterator = sortedFront.keySet().iterator();
                Integer prevKey = keyIterator.next();
                Integer key = -1;
                int i = 0;
                while(keyIterator.hasNext()){
                    key = i == 0 ? keyIterator.next() : key; // Only first key
                    if(keyIterator.hasNext()){
                        Integer nextKey = keyIterator.next();
                        double d = M == 0 ? 0 : crowdingDistance.get(key);
                        d += (sortedFront.get(prevKey)[M] - sortedFront.get(nextKey)[M]) / (fMax - fMin);
                        //d = crowdingDistance.get(key) != Double.MAX_VALUE ? d :  Double.MAX_VALUE;
                        crowdingDistance.put(key, d);
                        prevKey = key;
                        key = nextKey;
                        i++;
                    }
                    else{
                        break;
                    }
                }
            }
        }
        return crowdingDistance;
    }

    public static ArrayList<Map<Integer, double[]>> calculateFitness(ArrayList<Individual> population, BufferedImage image,
            int[][] neighborhood, double[][] rgbDistance, ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMaps) {
        double[] edgeValues = calculateEdgeValue(population, image, neighborhood, rgbDistance);
        double[] connectivityValues = calculateConnectivity(population, image, neighborhood, rgbDistance);
        double[] deviationValues = calculateOverallDeviation(population, image, neighborhood, segmentMaps);

        HashMap<Integer, double[]> valuesMap = new HashMap<Integer, double[]>();
        double minValue = Double.MAX_VALUE;
        for (int i = 0; i < edgeValues.length; i++) {
            valuesMap.put(i, new double[] { edgeValues[i], connectivityValues[i], deviationValues[i] });
            minValue = Math.min(minValue, edgeValues[i] + connectivityValues[i] +  deviationValues[i]);
        }
        System.out.println("Minimum sum value: " + minValue);

        // Map<Integer, double[]> sortedMap = new TreeMap<Integer, double[]>(new ObjectiveComparator(valuesMap, "Ascending", 2));
        // sortedMap.putAll(valuesMap);
        LinkedHashMap<Integer, double[]> sortedMap = ParetoComparator.sortMap(valuesMap,  "Ascending", 2);

        Map<Integer, double[]> paretoFront = new HashMap<Integer, double[]>();
        ArrayList<Map<Integer, double[]>> paretoFronts = new ArrayList<Map<Integer, double[]>>();

        int front = 1;
        while (sortedMap.size() > 0) {
            paretoFront = Utils.kungsParetoAlgorithm(sortedMap);
            paretoFronts.add(paretoFront);
            for (Integer key : paretoFront.keySet()) {
                population.get(key).paretoFront = front;
                sortedMap.remove(key);
            }
            front ++;
        }
        return paretoFronts;
    }

    // Maximize this
    public static double[] calculateEdgeValue(ArrayList<Individual> population, BufferedImage image,
            int[][] neighborhood, double[][] rgbDistance) {
        double[] edgeValues = new double[population.size()];
        for (int i = 0; i < population.size(); i++) {
            Individual individual = population.get(i);
            double distance = 0;
            for (int pixelIndex = 0; pixelIndex < image.getWidth() * image.getHeight(); pixelIndex++) {
                for (int neighboor = 0; neighboor < 8; neighboor++) {
                    if (neighborhood[pixelIndex][neighboor] != -1
                            && individual.segmentIds[pixelIndex] != individual.segmentIds[neighborhood[pixelIndex][neighboor]]) {
                        distance -= rgbDistance[pixelIndex][neighboor]; // use -= to minimize
                    }
                }
            }
            edgeValues[i] = distance;
        }
        return edgeValues;
    }

    // Minimize this
    public static double[] calculateConnectivity(ArrayList<Individual> population, BufferedImage image,
            int[][] neighborhood, double[][] rgbDistance) {
        double[] connectivityValues = new double[population.size()];
        for (int i = 0; i < population.size(); i++) {
            Individual individual = population.get(i);
            double connectivity = 0;
            for (int pixelIndex = 0; pixelIndex < image.getWidth() * image.getHeight(); pixelIndex++) {
                for (int neighboor = 0; neighboor < 8; neighboor++) {
                    if (neighborhood[pixelIndex][neighboor] != -1
                            && individual.segmentIds[pixelIndex] != individual.segmentIds[neighborhood[pixelIndex][neighboor]]) {
                        connectivity += (double) 1 / 8;
                    }
                }
            }
            connectivityValues[i] = connectivity;
        }
        return connectivityValues;
    }

    // Minimize
    public static double[] calculateOverallDeviation(ArrayList<Individual> population, BufferedImage image,
            int[][] neighborhood, ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMaps) {
        double[] deviationValues = new double[population.size()];
        for (int i = 0; i < population.size(); i++) {
            HashMap<Integer, ArrayList<Integer>> segments = segmentMaps.get(i);
            double deviation = 0;
            for (Integer key : segments.keySet()) {
                ArrayList<Integer> segmentPixels = segments.get(key);
                double[] averageValues = new double[] { 0.0, 0.0, 0.0 };
                // Calculate segment's average pixel RGB
                for (Integer pixel : segmentPixels) {
                    int[] coordinate = Utils.pixelIndexToCoordinate(image, pixel);
                    int color = image.getRGB(coordinate[0], coordinate[1]);
                    averageValues[0] += ((color & 0xff0000) >> 16) / segmentPixels.size(); // Red
                    averageValues[1] += ((color & 0xff00) >> 8) / segmentPixels.size(); // Green
                    averageValues[2] += (color & 0xff) / segmentPixels.size(); // Blue
                }
                // Calculate distance to average pixel (Compactness)
                for (Integer pixel : segmentPixels) {
                    int[] coordinate = Utils.pixelIndexToCoordinate(image, pixel);
                    int color = image.getRGB(coordinate[0], coordinate[1]);
                    // Components will be in the range of 0..255:
                    int red = (color & 0xff0000) >> 16;
                    int green = (color & 0xff00) >> 8;
                    int blue = color & 0xff;
                    deviation += Math.sqrt(Math.pow((red - averageValues[0]), 2)
                            + Math.pow((green - averageValues[1]), 2) + Math.pow((blue - averageValues[2]), 2));
                }
            }
            deviationValues[i] = deviation;
        }
        return deviationValues;
    }

}
