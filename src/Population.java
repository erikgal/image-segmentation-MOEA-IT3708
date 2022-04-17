package src;

import java.util.ArrayList;

import java.awt.image.BufferedImage;

public class Population {

    public static ArrayList<Individual> generateRandomPopulation(BufferedImage image, int populationSize,
            int[][] neighborhood) {
        ArrayList<Individual> population = new ArrayList<Individual>(populationSize);
        for (int i = 0; i < populationSize; i++) {
            Individual ind = new Individual();
            ArrayList<Integer> pixelDirection = new ArrayList<Integer>();
            for (int pixelIndex = 0; pixelIndex < image.getWidth() * image.getHeight(); pixelIndex++) {
                int neighboor = -1;
                int direction = 0;
                // Lazy dumb solution
                while (neighboor == -1) {
                    direction = (int) (Math.random() * Utils.PixelDirection.values().length - 1);
                    neighboor = neighborhood[pixelIndex][direction];
                }
                pixelDirection.add(direction);
            }
            ind.pixelDirection = pixelDirection;
            ind.populationIndex = i;
            population.add(ind);
        }
        return population;
    }

    public static ArrayList<Individual> generateMSTPopulation(BufferedImage image, int populationSize,
            int[][] neighborhood, double[][] rgbDistance) {
        ArrayList<Individual> population = new ArrayList<Individual>(populationSize);
       
        MST mst = new MST(image);
        mst.designMST(neighborhood, rgbDistance);   
        
        for (int i = 0; i < populationSize; i++) {
            Individual ind = new Individual();
            ArrayList<Integer> pixelDirection = new ArrayList<Integer>();

            int startPixel = (int) (Math.random() * (image.getWidth() * image.getHeight()));

        }

        return population;
    }

    public static int[] generateNeighborhood(int pixelIndex, BufferedImage image, ArrayList<Integer> omit) {
        int[] neighborhood = new int[8];
        for (int i = 0; i < 8; i++) {
            if (omit.contains(new Integer(i))) {
                neighborhood[i] = -1;
            } else if (i == 0) { // Right neighbor
                neighborhood[i] = pixelIndex + 1;
            }

            else if (i == 1) { // Left neighbor
                neighborhood[i] = pixelIndex - 1;
            }

            else if (i == 2) { // Top neighbor
                neighborhood[i] = pixelIndex - image.getWidth();
            }

            else if (i == 3) { // Bottom neighbor
                neighborhood[i] = pixelIndex + image.getWidth();
            }

            else if (i == 4) { // Top-Right neighbor
                neighborhood[i] = pixelIndex + 1 - image.getWidth();
            }

            else if (i == 5) { // Bottom-Right neighbor
                neighborhood[i] = pixelIndex + 1 + image.getWidth();
            }

            else if (i == 6) { // Top-Left neighbor
                neighborhood[i] = pixelIndex - 1 - image.getWidth();
            }

            else if (i == 7) { // Bottom-Right neighbor
                neighborhood[i] = pixelIndex - 1 + image.getWidth();
            }
        }
        return neighborhood;
    }

    public static int[][] generateNeighborhoodMatrix(BufferedImage image) {
        int[][] neighborhoodMatrix = new int[image.getWidth() * image.getHeight()][8];
        for (int pixelIndex = 0; pixelIndex < image.getWidth() * image.getHeight(); pixelIndex++) {
            Boolean topRow = pixelIndex < image.getWidth();
            Boolean bottomRow = pixelIndex > (image.getWidth() * (image.getHeight() - 1)) - 1;
            Boolean leftCol = pixelIndex % image.getWidth() == 0;
            Boolean rightCol = (pixelIndex + 1) % image.getWidth() == 0;

            ArrayList<Integer> omit = new ArrayList<Integer>();

            // Pixels in Top Row
            if (topRow) {
                omit.add(6);
                omit.add(2);
                omit.add(4);
                // Pixel in Top-Left corner
                if (leftCol) {
                    omit.add(1);
                    omit.add(7);
                }
                // Pixel in Top-Right corner
                else if (rightCol) {
                    omit.add(0);
                    omit.add(5);
                }
            } else if (bottomRow) {
                omit.add(7);
                omit.add(3);
                omit.add(5);
                // Pixel in Bottom-Left corner
                if (leftCol) {
                    omit.add(1);
                    omit.add(6);
                }
                // Pixel in Bottom-Right corner
                else if (rightCol) {
                    omit.add(0);
                    omit.add(4);
                }
            }
            // Pixel in middle left column
            else if (leftCol) {
                omit.add(6);
                omit.add(1);
                omit.add(7);
            }
            // Pixel in middle right column
            else if (rightCol) {
                omit.add(4);
                omit.add(0);
                omit.add(5);
            }
            neighborhoodMatrix[pixelIndex] = generateNeighborhood(pixelIndex, image, omit);
        }
        return neighborhoodMatrix;
    }

    public static double[][] generateEuclideanRGBDistance(BufferedImage image, int[][] neighborhood) {
        double[][] euclideanRGBDistances = new double[image.getWidth() * image.getHeight()][8];
        for (int pixelIndex = 0; pixelIndex < image.getWidth() * image.getHeight(); pixelIndex++) {
            int[] coordinate = Utils.pixelIndexToCoordinate(image, pixelIndex);
            int color = image.getRGB(coordinate[0], coordinate[1]);
            // Components will be in the range of 0..255:
            int red = (color & 0xff0000) >> 16;
            int green = (color & 0xff00) >> 8;
            int blue = color & 0xff;

            for (int neighboorIndex = 0; neighboorIndex < 8; neighboorIndex++) {
                if (neighborhood[pixelIndex][neighboorIndex] != -1) {
                    int[] neighboorCoordinate = Utils.pixelIndexToCoordinate(image,
                            neighborhood[pixelIndex][neighboorIndex]);
                    int neighboorColor = image.getRGB(neighboorCoordinate[0], neighboorCoordinate[1]);
                    // Components will be in the range of 0..255:
                    int neighboorRed = (neighboorColor & 0xff0000) >> 16;
                    int neighboorGreen = (neighboorColor & 0xff00) >> 8;
                    int neighboorBlue = neighboorColor & 0xff;

                    double euclideanRGBDistance = Math.sqrt(Math.pow((red - neighboorRed), 2)
                            + Math.pow((green - neighboorGreen), 2) + Math.pow((blue - neighboorBlue), 2));
                    euclideanRGBDistances[pixelIndex][neighboorIndex] = euclideanRGBDistance;
                } else {
                    euclideanRGBDistances[pixelIndex][neighboorIndex] = -1;
                }
            }

        }
        return euclideanRGBDistances;
    }
}
