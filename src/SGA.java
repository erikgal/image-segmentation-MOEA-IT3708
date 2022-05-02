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

class SGA {
        public static void main(String[] args) {
                // Hyper-parameters
                int epochs = 100;
                int imageIndex = 3;
                int populationSize = 10;
                double pC = 0.1;
                double pM = 0.0007;
                int minSegments = 12;
                int maxSegments = 47;
                boolean checkEarlyStopping = true;
                int earlyStopRound = 2;
                double threshold = 80.00;

                double edgeWeight = 0.01;
                double connectivityWeight = 7.5;
                double deviationWeight = 0.001;
                int nbrOutput = 5;

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
                        ArrayList<Double> weightedFitness = Fitness.calculateWeightedFitness(population, image,
                                        neighborhood,
                                        rgbDistance, segmentMaps, edgeWeight, connectivityWeight, deviationWeight);

                        // Find Parents and offspring (P & Q)
                        ArrayList<Individual> parents = EAUtils.tournamentSelectionSGA(population, weightedFitness);
                        ArrayList<Individual> offspring = EAUtils.createOffspring(parents, neighborhood, pC, pM);
                        population.addAll(offspring);

                        // Calculate information for new 2N population
                        disjointSet = Utils.fillDisjointUnionSet(population, image, neighborhood);
                        segmentMaps = Utils.getSegementMaps(disjointSet, image.getWidth() * image.getHeight(),
                                        population);
                        weightedFitness = Fitness.calculateWeightedFitness(population, image,
                                        neighborhood,
                                        rgbDistance, segmentMaps, edgeWeight, connectivityWeight, deviationWeight);

                        // Finally, select survivors based on weighted fitness
                        population = EAUtils.survivorSelectionSGA(populationSize, population, weightedFitness);
                }

                DisjointUnionSet[] disjointSet = Utils.fillDisjointUnionSet(population, image, neighborhood);
                ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMaps = Utils.getSegementMaps(disjointSet,
                                image.getWidth() * image.getHeight(), population);
                ArrayList<Double> weightedFitness = Fitness.calculateWeightedFitness(population, image,
                                neighborhood,
                                rgbDistance, segmentMaps, edgeWeight, connectivityWeight, deviationWeight);

                population = EAUtils.survivorSelectionSGA(nbrOutput, population, weightedFitness);
                disjointSet = Utils.fillDisjointUnionSet(population, image, neighborhood);
                segmentMaps = Utils.getSegementMaps(disjointSet, image.getWidth() * image.getHeight(), population);
                ArrayList<BufferedImage> outputImages = Utils.createImageSGA(image, population, disjointSet, segmentMaps, neighborhood);
                Utils.saveImage(outputImages, new Boolean(false));
        }
}