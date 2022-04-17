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


class NSGA2 {
    public static void main(String[] args) {
         // Hyper-parameters
        int epochs = 360000;
        int imageIndex = 0;
        int populationSize = 50;
        double pC = 0.1;
        double pM = 0.007;

        BufferedImage[] images = Utils.loadImages();
        BufferedImage image = images[imageIndex];
        int[][] neighborhood = Population.generateNeighborhoodMatrix(image);
        double[][] rgbDistance = Population.generateEuclideanRGBDistance(image, neighborhood);

        ArrayList<Individual> population = Population.generateRandomPopulation(image, populationSize, neighborhood);
        // ArrayList<Individual> population = Population.generateMSTPopulation(image, populationSize, neighborhood, rgbDistance);


        for (int epoch = 1; epoch < epochs; epoch++) {

            // Calculate information about new population
            System.out.println("Epoch: " + epoch);
            DisjointUnionSet[] disjointSet = Utils.fillDisjointUnionSet(population, image, neighborhood);
            ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMaps = Utils.getSegementMaps(disjointSet, image.getWidth() * image.getHeight(), population);
            ArrayList<Map<Integer, double[]>> paretoFronts = Fitness.calculateFitness(population, image, neighborhood, rgbDistance, segmentMaps);
            HashMap<Integer, Double> crowdingDistances = Fitness.calculateCrowdingDistance(paretoFronts);

            // Find Parents and offspring (P & Q)
            ArrayList<Individual> parents = EAUtils.tournamentSelection(population, paretoFronts, crowdingDistances);
            ArrayList<Individual> offspring = EAUtils.createOffspring(parents, neighborhood, pC, pM);
            population.addAll(offspring);

            // Calculate information for new 2N population
            disjointSet = Utils.fillDisjointUnionSet(population, image, neighborhood);
            segmentMaps = Utils.getSegementMaps(disjointSet, image.getWidth() * image.getHeight(), population);
            paretoFronts = Fitness.calculateFitness(population, image, neighborhood, rgbDistance, segmentMaps);
            crowdingDistances = Fitness.calculateCrowdingDistance(paretoFronts);

            // Number og segment evalutation
            Integer min = Integer.MAX_VALUE;
            for(HashMap<Integer, ArrayList<Integer>> segmentMap : segmentMaps){
                min = Math.min(min, segmentMap.keySet().size());
            }
            System.out.println("Number og segments: " + min);

            // Finally, select survivors based on pareto front and crowding distance
            population = EAUtils.survivorSelection(populationSize, population, paretoFronts, crowdingDistances);
        }   

        DisjointUnionSet[] disjointSet = Utils.fillDisjointUnionSet(population, image, neighborhood);
        ArrayList<HashMap<Integer, ArrayList<Integer>>> segmentMaps = Utils.getSegementMaps(disjointSet, image.getWidth() * image.getHeight(), population);
        ArrayList<Map<Integer, double[]>> paretoFronts = Fitness.calculateFitness(population, image, neighborhood, rgbDistance, segmentMaps);
        HashMap<Integer, Double> crowdingDistances = Fitness.calculateCrowdingDistance(paretoFronts);


    }
}