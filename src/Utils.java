package src;

import java.io.*;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Paint;

import javax.imageio.ImageIO;
import javax.print.event.PrintJobListener;

public class Utils {
    public static enum PixelDirection {
        NONE, RIGHT, LEFT, UP, DOWN, TOP_RIGHT, BOTTOM_RIGHT, TOP_LEFT, BOTTOM_LEFT
    }

    public static BufferedImage[] loadImages() {
        String[] imageNames = { "86016", "118035", "147091", "176035", "176039", "353013" };
        BufferedImage[] images = new BufferedImage[6];
        int counter = 0;
        for (String imageName : imageNames) {
            File imagePath = new File("training_images/" + imageName + "/Test image.jpg");
            try {
                BufferedImage image = ImageIO.read(imagePath);
                images[counter] = image;
            } catch (IOException e) {
                e.printStackTrace();
                throw new Error("Cannot open the image: " + imagePath);
            }
            counter++;
        }
        return images;
    }

    public static DisjointUnionSet[] fillDisjointUnionSet(ArrayList<Individual> population, BufferedImage image,
            int[][] neighborhood) {
        DisjointUnionSet[] disjointSets = new DisjointUnionSet[population.size()];
        int indIndex = 0;
        for (Individual ind : population) {
            DisjointUnionSet disjointSet = new DisjointUnionSet(image.getWidth() * image.getHeight());
            int pixelIndex = 0;
            for (Integer direction : ind.pixelDirection) {
                if (direction != -1) {
                    int childPixel = neighborhood[pixelIndex][direction];
                    disjointSet.union(pixelIndex, childPixel);
                }
                pixelIndex++;
            }
            disjointSets[indIndex] = disjointSet;
            indIndex++;
        }
        return disjointSets;
    }

    public static ArrayList<HashMap<Integer, ArrayList<Integer>>> getSegementMaps(DisjointUnionSet[] disjointSets,
            int N, ArrayList<Individual> population) {
        ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMaps = new ArrayList<HashMap<Integer, ArrayList<Integer>>>();
        for (int i = 0; i < disjointSets.length; i++) {
            DisjointUnionSet disjointSet = disjointSets[i];
            Individual individual = population.get(i);
            int[] segmentIds = new int[N];

            HashMap<Integer, ArrayList<Integer>> segmentMap = new HashMap<>();
            for (int pixelIndex = 0; pixelIndex < N; pixelIndex++) {
                int key = disjointSet.find(pixelIndex);
                segmentIds[pixelIndex] = key;
                if (segmentMap.containsKey(key)) {
                    segmentMap.get(key).add(pixelIndex);
                    continue;
                }
                ArrayList<Integer> tempList = new ArrayList<>();
                tempList.add(pixelIndex);
                segmentMap.put(key, tempList);
            }
            individual.segmentIds = segmentIds;
            segmentMaps.add(segmentMap);
        }
        return segmentMaps;
    }

    public static int[] pixelIndexToCoordinate(BufferedImage image, int pixelIndex) {
        int x = pixelIndex % image.getWidth();
        int y = (int) pixelIndex / image.getWidth();
        return new int[] { x, y };
    }

    public static LinkedHashMap<Integer, double[]> kungsParetoAlgorithm(Map<Integer, double[]> P) {
        if (P.size() == 1) {
            // If front only has 1 element, don't return a TreeMap, causes later issues
            LinkedHashMap<Integer, double[]> M = new LinkedHashMap<Integer, double[]>();
            Integer key = P.keySet().iterator().next();
            M.put(key, P.get(key));
            return M;
        }
        HashMap<Integer, double[]> tInput = new HashMap<Integer, double[]>();
        HashMap<Integer, double[]> bInput = new HashMap<Integer, double[]>();

        Iterator<Integer> keyIterator = P.keySet().iterator();
        for (int i = 0; i < (int) P.size() / 2; i++) {
            Integer key = keyIterator.next();
            tInput.put(key, P.get(key));
        }
        for (int i = (int) (P.size() / 2) + 1; i <= P.size(); i++) {
            Integer key = keyIterator.next();
            bInput.put(key, P.get(key));
        }

        Map<Integer, double[]> T = kungsParetoAlgorithm(tInput);
        Map<Integer, double[]> B = kungsParetoAlgorithm(bInput);

        LinkedHashMap<Integer, double[]> M = new LinkedHashMap<Integer, double[]>();

        for (Integer keyB : B.keySet()) {
            double[] possibleSolution = B.get(keyB);
            boolean dominated = false;
            for (Integer keyT : T.keySet()) {
                double[] solution = T.get(keyT);
                // Check for non-dominated possibleSolution is dominated by non-dominated
                // solution
                if (possibleSolution[0] > solution[0] && possibleSolution[1] > solution[1]
                        && possibleSolution[2] > solution[2]) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) {
                M.put(keyB, possibleSolution);
            }
        }
        // The i-th non-dominated solution of B is not dominated by any non-dominated
        // solution of T
        // => M = T Unioun {i}
        for (Integer keyT : T.keySet()) {
            M.put(keyT, P.get(keyT));
        }
        return M;
    }

    public static void addNewEdge(int newPixel, PriorityQueue<Double[]> edgeQueue, int[][] neighborhood,
            double[][] rgbDistance, Boolean isPixelVisited[]) {
        for (int neighbor = 0; neighbor < 8; neighbor++) {
            int toPixel = neighborhood[newPixel][neighbor];
            if (toPixel != -1 && !isPixelVisited[toPixel]) {
                // [from pixel, to pixel, rgbValue, neighborhood direction]
                edgeQueue.add(new Double[] { (double) newPixel, (double) toPixel, rgbDistance[newPixel][neighbor],
                        (double) neighbor });
            }
        }
    }

    public static ArrayList<Integer> primMST(int[][] neighborhood, double[][] rgbDistance, int startPixel,
            HashMap<Integer, Double> worstEdges) {
        int N = neighborhood.length;
        ArrayList<Integer> pixelDirections = new ArrayList<Integer>(N);
        int[] oppositeNeighborhood = new int[] { 1, 0, 3, 2, 7, 6, 5, 4 };

        // HashMaps <PixelId, Edge>
        PriorityQueue<Double[]> edgeQueue = new PriorityQueue<Double[]>(new primComparator());

        Boolean isPixelVisited[] = new Boolean[N];
        for (int i = 0; i < N; i++) {
            isPixelVisited[i] = false;
            pixelDirections.add(-1);
        }

        // Initiate prim's algorithm with root pixel
        isPixelVisited[startPixel] = true;
        pixelDirections.set(startPixel, -1);
        Utils.addNewEdge(startPixel, edgeQueue, neighborhood, rgbDistance, isPixelVisited);

        while (edgeQueue.size() > 0) {
            Double[] list = edgeQueue.poll();
            Integer newPixel = list[1].intValue();
            if (!isPixelVisited[newPixel]) {
                Integer visitedPixel = list[0].intValue();
                Double distance = list[2];
                Integer neighbor = list[3].intValue();

                isPixelVisited[newPixel] = true;
                pixelDirections.set(newPixel, oppositeNeighborhood[neighbor]); // Neighbor in opposite
                                                                               // direction

                Utils.addNewEdge(newPixel, edgeQueue, neighborhood, rgbDistance, isPixelVisited);
                worstEdges.put(newPixel, distance);
            }
        }

        return pixelDirections;
    }

    public static boolean[] filterResults(ArrayList<Individual> population,
            DisjointUnionSet[] disjointSet,
            ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMap, ArrayList<Map<Integer, double[]>> paretoFronts,
            int[][] neighborhood, int floor, int roof) {

        int N = paretoFronts.get(0).size();
        boolean[] isFeasible = new boolean[population.size()];
        Iterator<Integer> iterator = paretoFronts.get(0).keySet().iterator();

        for (int i = 0; i < N; i++) {
            Integer individualIndex = iterator.next();
            HashMap<Integer, ArrayList<Integer>> segments = segmentMap.get(individualIndex);
            if (floor <= segments.keySet().size() && segments.keySet().size() <= roof) {
                isFeasible[individualIndex] = true;
            } else {
                isFeasible[individualIndex] = false;
            }
        }
        return isFeasible;

    }

    public static ArrayList<BufferedImage> createImage(BufferedImage image, ArrayList<Individual> population,
            DisjointUnionSet[] disjointSet,
            ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMap, ArrayList<Map<Integer, double[]>> paretoFronts,
            int[][] neighborhood, boolean[] isFeasible) {

        int N = paretoFronts.get(0).size();
        ArrayList<BufferedImage> bufferedImages = new ArrayList<BufferedImage>(N);
        Iterator<Integer> iterator = paretoFronts.get(0).keySet().iterator();

        for (int i = 0; i < N; i++) {
            BufferedImage type1 = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            BufferedImage type2 = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            Integer individualIndex = iterator.next();
            if (isFeasible[individualIndex]) {

                Individual individual = population.get(individualIndex);

                Graphics2D graphics1 = (Graphics2D) type1.getGraphics();
                graphics1.drawImage(image, 0, 0, null);
                graphics1.dispose();

                Graphics2D graphics = type2.createGraphics();
                graphics.setPaint(new Color(255, 255, 255));
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                graphics.dispose();

                for (int pixelIndex = 0; pixelIndex < image.getWidth() * image.getHeight(); pixelIndex++) {
                    for (int neighboor = 0; neighboor < 8; neighboor++) {
                        if (neighborhood[pixelIndex][neighboor] == -1 // Edge pixel
                                || individual.segmentIds[pixelIndex] != individual.segmentIds[neighborhood[pixelIndex][neighboor]]) {// Segment
                                                                                                                                     // edge
                                                                                                                                     // pixel
                            int[] coordinates = Utils.pixelIndexToCoordinate(image, pixelIndex);
                            type1.setRGB(coordinates[0], coordinates[1], new Color(0, 255, 0).getRGB());
                            type2.setRGB(coordinates[0], coordinates[1], new Color(0, 0, 0).getRGB());
                        }
                    }
                }
                bufferedImages.add(type1);
                bufferedImages.add(type2);
            }
        }
        return bufferedImages;
    }

    public static ArrayList<BufferedImage> createImageSGA(BufferedImage image, ArrayList<Individual> population,
            DisjointUnionSet[] disjointSet,
            ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMap,
            int[][] neighborhood) {
        ArrayList<BufferedImage> bufferedImages = new ArrayList<BufferedImage>(population.size());

        for (int i = 0; i < population.size(); i++) {
            BufferedImage type1 = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            BufferedImage type2 = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

            Individual individual = population.get(i);

            Graphics2D graphics1 = (Graphics2D) type1.getGraphics();
            graphics1.drawImage(image, 0, 0, null);
            graphics1.dispose();

            Graphics2D graphics2 = type2.createGraphics();
            graphics2.setPaint(new Color(255, 255, 255));
            graphics2.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics2.dispose();

            for (int pixelIndex = 0; pixelIndex < image.getWidth() * image.getHeight(); pixelIndex++) {
                for (int neighboor = 0; neighboor < 8; neighboor++) {
                    if (neighborhood[pixelIndex][neighboor] == -1 // Edge pixel
                            || individual.segmentIds[pixelIndex] != individual.segmentIds[neighborhood[pixelIndex][neighboor]]) {// Segment
                                                                                                                                 // edge
                                                                                                                                 // pixel
                        int[] coordinates = Utils.pixelIndexToCoordinate(image, pixelIndex);

                        type1.setRGB(coordinates[0], coordinates[1], new Color(0, 255, 0).getRGB());
                        type2.setRGB(coordinates[0], coordinates[1], new Color(0, 0, 0).getRGB());
                    }
                }
            }
            bufferedImages.add(type1);
            bufferedImages.add(type2);

        }

        return bufferedImages;
    }

    public static void saveImage(ArrayList<BufferedImage> bufferedImages, boolean earlyStopping) {
        for (int i = 0; i < bufferedImages.size(); i++) {
            try {
                File outputFile;
                if (i % 2 == 0) {
                    outputFile = new File("./src/images/Type1/image" + (i+1) + ".png");
                } else if (earlyStopping) {
                    outputFile = new File("./src/images/Student_Segmentation_Files/image" + i + ".png");
                } else {
                    outputFile = new File("./src/evaluator/Student_Segmentation_Files/image" + i + ".png");
                }
                outputFile.getParentFile().mkdirs();
                ImageIO.write(bufferedImages.get(i), "png", outputFile);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    public static ArrayList<Double> earlyStopping() {
        String s = null;
        File f = new File("src");

        ArrayList<Double> scores = new ArrayList<Double>();

        try {
            Process p = Runtime.getRuntime().exec("python3 " + f.getAbsolutePath() + "/evaluator/run.py earlyStopping");

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // read the output from the command
            int i = 0;
            while ((s = stdInput.readLine()) != null) {
                s = s.replaceAll("[^-?0-9.]+", " ");
                scores.add(Double.parseDouble(Arrays.asList(s.trim().split(" ")).get(0)));
                i++;
            }

            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

            // System.exit(0);
        } catch (IOException e) {
            System.out.println("exception happened - here's what I know: ");
            e.printStackTrace();
            System.exit(-1);
        }

        return scores;
    }

    public static void printScore(int index, ArrayList<Individual> population,
    BufferedImage image,
    int[][] neighborhood, double[][] rgbDistance, ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMaps,  boolean[] isFeasible, ArrayList<Map<Integer, double[]>> paretoFronts){
        
        int counter = 0;
        for (int i = 0; i < isFeasible.length; i++) {
            if (isFeasible[i]){
                counter ++;
            }   
            if (counter == index){
                Map<Integer, double[]> front = paretoFronts.get(0);
                double[] fitness  = front.get(counter);
                System.out.println("Edge: " + fitness[0] + ", Connectivity: " + fitness[1] + ", Deviation: " + fitness[2]);
                break;
            }
        }
    }
}
