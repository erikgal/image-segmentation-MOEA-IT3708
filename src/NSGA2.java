package src;

import java.io.File;
import java.io.IOException;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import java.io.*;

class NSGA2 {
        public static void main(String[] args) {
                // Hyper-parameters
                int epochs = 300;
                int imageIndex = 3;
                int populationSize = 10;
                double pC = 0.1;
                double pM = 0.0007;
                int minSegments = 12;
                int maxSegments = 47;
                boolean checkEarlyStopping = true;
                int earlyStopRound = 2;
                double threshold = 75.00;

                BufferedImage[] images = Utils.loadImages();
                BufferedImage image = images[imageIndex];
                int[][] neighborhood = Population.generateNeighborhoodMatrix(image);
                double[][] rgbDistance = Population.generateEuclideanRGBDistance(image, neighborhood);

                ArrayList<Individual> population = Population.generateMSTPopulation(image, populationSize, neighborhood,
                                rgbDistance);

                for (int epoch = 1; epoch < epochs; epoch++) {

                        // Calculate information about new population
                        System.out.println("Epoch: " + epoch);
                        DisjointUnionSet[] disjointSet = Utils.fillDisjointUnionSet(population, image, neighborhood);
                        ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMaps = Utils.getSegementMaps(disjointSet,
                                        image.getWidth() * image.getHeight(), population);
                        ArrayList<Map<Integer, double[]>> paretoFronts = Fitness.calculateFitness(population, image,
                                        neighborhood,
                                        rgbDistance, segmentMaps);
                        HashMap<Integer, Double> crowdingDistances = Fitness.calculateCrowdingDistance(paretoFronts);

                        if (checkEarlyStopping && epoch % earlyStopRound == 0) {
                                boolean[] isFeasible = Utils.filterResults(population, disjointSet, segmentMaps,
                                                paretoFronts, neighborhood, minSegments, maxSegments);
                                ArrayList<BufferedImage> outputImages = Utils.createImage(image, population,
                                                disjointSet, segmentMaps, paretoFronts, neighborhood, isFeasible);
                                Utils.saveImage(outputImages, new Boolean(true));

                                ArrayList<Double> scores = Utils.earlyStopping();
                                double max = Double.MIN_VALUE;
                                int winning_index = -1;
                                for (int i = 0; i < scores.size(); i++) {
                                        if(scores.get(i) >= max){
                                                winning_index = i;
                                                max = scores.get(i);
                                        }
                                }
                                if(winning_index != -1 && scores.get(winning_index) >= threshold){
                                        System.out.println("Found solution with " + scores.get(winning_index) + "%");
                                        ArrayList<BufferedImage> bestImage = new ArrayList<BufferedImage>();
                                        int index = winning_index * 2;
                                        bestImage.add(outputImages.get(index));
                                        bestImage.add(outputImages.get(index+1));
                                        Utils.saveImage(bestImage, new Boolean(false));
                                        System.exit(0);
                                }
                                System.out.println("Best Score this generation: " + max);
                        }

                        // Find Parents and offspring (P & Q)
                        ArrayList<Individual> parents = EAUtils.tournamentSelection(population, paretoFronts,
                                        crowdingDistances);
                        ArrayList<Individual> offspring = EAUtils.createOffspring(parents, neighborhood, pC, pM);
                        population.addAll(offspring);

                        // Calculate information for new 2N population
                        disjointSet = Utils.fillDisjointUnionSet(population, image, neighborhood);
                        segmentMaps = Utils.getSegementMaps(disjointSet, image.getWidth() * image.getHeight(),
                                        population);
                        paretoFronts = Fitness.calculateFitness(population, image, neighborhood, rgbDistance,
                                        segmentMaps);
                        crowdingDistances = Fitness.calculateCrowdingDistance(paretoFronts);

                        // Finally, select survivors based on pareto front and crowding distance
                        population = EAUtils.survivorSelection(populationSize, population, paretoFronts,
                                        crowdingDistances);
                }

                DisjointUnionSet[] disjointSet = Utils.fillDisjointUnionSet(population, image, neighborhood);
                ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMaps = Utils.getSegementMaps(disjointSet,
                                image.getWidth() * image.getHeight(), population);
                ArrayList<Map<Integer, double[]>> paretoFronts = Fitness.calculateFitness(population, image,
                                neighborhood,
                                rgbDistance, segmentMaps);
                HashMap<Integer, Double> crowdingDistances = Fitness.calculateCrowdingDistance(paretoFronts);

                boolean[] isFeasible = Utils.filterResults(population, disjointSet, segmentMaps, paretoFronts,
                                neighborhood, minSegments, maxSegments);
                ArrayList<BufferedImage> outputImages = Utils.createImage(image, population, disjointSet, segmentMaps,
                                paretoFronts, neighborhood, isFeasible);
                Utils.saveImage(outputImages, new Boolean(false));
        }
}