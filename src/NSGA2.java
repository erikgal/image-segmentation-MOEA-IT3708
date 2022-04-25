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
                int epochs = 30;
                int imageIndex = 0;
                int populationSize = 10;
                double pC = 0.1;
                double pM = 0.0007;
                int minSegments = 4;
                int maxSegments = 41;
                boolean checkEarlyStopping = true;
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

                        if (checkEarlyStopping) {
                                boolean[] isFeasible = Utils.filterResults(population, disjointSet, segmentMaps,
                                                paretoFronts, neighborhood, minSegments, maxSegments);
                                ArrayList<BufferedImage> outputImages = Utils.createImage(image, population,
                                                disjointSet, segmentMaps, paretoFronts, neighborhood, isFeasible);
                                Utils.saveImage(outputImages);
                                ArrayList<Double> scores = Utils.earlyStopping();
                                double max = Double.MIN_VALUE;
                                for (int i = 0; i < scores.size(); i++) {
                                        max = Math.max(max, scores.get(i));
                                        if(scores.get(i) >= threshold){
                                                System.out.println("Found solution with " + scores.get(i) + "%");
                                                System.exit(0);
                                        }
                                }
                                System.out.println("Best Score this generation: " + max);
                        }

                        // Number og segment evalutation
                        double sum = 0.0;
                        for (HashMap<Integer, ArrayList<Integer>> segmentMap : segmentMaps) {
                                sum += segmentMap.keySet().size();
                        }
                        System.out.println("Average number og segments: " + sum / segmentMaps.size());

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
                Utils.saveImage(outputImages);
        }
}