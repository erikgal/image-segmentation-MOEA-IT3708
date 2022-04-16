package src;

import java.io.File;
import java.io.IOException;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
                disjointSet.union(pixelIndex, neighborhood[pixelIndex][direction]);
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
            segmentMaps.add(segmentMap);
            individual.segmentIds = segmentIds;
        }
        return segmentMaps;
    }

    public static int[] pixelIndexToCoordinate(BufferedImage image, int pixelIndex) {
        int x = pixelIndex % image.getWidth();
        int y = (int) pixelIndex / image.getWidth();
        return new int[] { x, y };
    }

    public static Map<Integer, double[]> kungsParetoAlgorithm(Map<Integer, double[]> P) {
        if (P.size() == 1) {
            // If front only has 1 element, don't return a TreeMap, causes later issues
            HashMap<Integer, double[]> M = new HashMap<Integer, double[]>();
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

        HashMap<Integer, double[]> M = new HashMap<Integer, double[]>();

        for (Integer keyB : B.keySet()) {
            double[] possibleSolution = B.get(keyB);
            boolean dominated = false;
            for (Integer keyT : T.keySet()) {
                double[] solution = T.get(keyT);
                // Check for non-dominated possibleSolution is dominated by non-dominated solution
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
        // The i-th non-dominated solution of B is not dominated by any non-dominated solution of T
        // => M = T Unioun {i}
        for (Integer keyT : T.keySet()) {
            M.put(keyT, P.get(keyT));
        }
        return M;
    }
}
