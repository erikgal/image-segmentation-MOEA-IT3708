package src;

import java.io.File;
import java.io.IOException;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Paint;

import javax.imageio.ImageIO;

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

    public static ArrayList<Integer> primMST(int[][] neighborhood, double[][] rgbDistance, int startPixel, HashMap<Integer, Double> worstEdges) {
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
                                                                               // directio

                Utils.addNewEdge(newPixel, edgeQueue, neighborhood, rgbDistance, isPixelVisited);
                worstEdges.put(newPixel, distance);
            }
        }

        return pixelDirections;
    }

    public static BufferedImage[] createImage(BufferedImage image, ArrayList<Individual> population,
            DisjointUnionSet[] disjointSet,
            ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMap, ArrayList<Map<Integer, double[]>> paretoFronts,
            int[][] neighborhood) {

        int N = paretoFronts.get(0).size();
        BufferedImage[] bufferedImages = new BufferedImage[N];
        Iterator<Integer> iterator = paretoFronts.get(0).keySet().iterator();

        for (int i = 0; i < N; i++) {
            BufferedImage type2 = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            Integer individualIndex = iterator.next();
            Individual individual = population.get(individualIndex);

            // Initialize the entire type 2 image as white
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
                        type2.setRGB(coordinates[0], coordinates[1], new Color(0, 0, 0).getRGB());
                    }
                }
            }
            bufferedImages[i] = type2;
        }
        return bufferedImages;
    }

    public static void saveImage(BufferedImage[] bufferedImages) {
        for (int i = 0; i < bufferedImages.length; i++) {
            try {
                File outputFile = new File("./src/evaluator/Student_Segmentation_Files/image" + i + ".png");
                outputFile.getParentFile().mkdirs();
                ImageIO.write(bufferedImages[i], "png", outputFile);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}
