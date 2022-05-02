package src;

import java.util.*;
import java.lang.*;

public class EAUtils {

    public static ArrayList<Individual> tournamentSelection(ArrayList<Individual> population,
            ArrayList<Map<Integer, double[]>> paretoFronts, HashMap<Integer, Double> crowdingDistances) {
        ArrayList<Individual> parents = new ArrayList<Individual>(population.size());

        for (int i = 0; i < ((int) population.size() / 2) * 2; i++) {
            int cand1Index = (int) (Math.random() * population.size());
            int cand2Index = (int) (Math.random() * population.size());
            Individual candidate1 = population.get(cand1Index);
            Individual candidate2 = population.get(cand2Index);
            Individual winner;

            if (candidate1.paretoFront == candidate2.paretoFront) {
                winner = crowdingDistances.get(cand1Index) >= crowdingDistances.get(cand1Index)
                        ? candidate1
                        : candidate2;
            } else {
                winner = candidate1.paretoFront > candidate2.paretoFront ? candidate2 : candidate2;
            }
            parents.add(winner);
        }

        return parents;
    }

    public static ArrayList<Individual> tournamentSelectionSGA(ArrayList<Individual> population,
            ArrayList<Double> fitness) {
        ArrayList<Individual> parents = new ArrayList<Individual>(population.size());

        for (int i = 0; i < ((int) population.size() / 2) * 2; i++) {
            int cand1Index = (int) (Math.random() * population.size());
            int cand2Index = (int) (Math.random() * population.size());
            Individual candidate1 = population.get(cand1Index);
            Individual candidate2 = population.get(cand2Index);
            Individual winner = fitness.get(cand1Index) < fitness.get(cand2Index) ? candidate1 : candidate2;

            parents.add(winner);
        }

        return parents;
    }

    public static ArrayList<Individual> survivorSelection(int N, ArrayList<Individual> population,
            ArrayList<Map<Integer, double[]>> paretoFronts, HashMap<Integer, Double> crowdingDistances) {

        ArrayList<Individual> survivors = new ArrayList<Individual>(N);
        for (Map<Integer, double[]> paretoFront : paretoFronts) {
            if (survivors.size() + paretoFront.size() <= N) {
                for (Integer key : paretoFront.keySet()) {
                    population.get(key).populationIndex = survivors.size();
                    survivors.add(population.get(key));
                }
            } else {
                Integer remainingCapacity = N - survivors.size();

                // Get crowding distances for desired front, and sort them
                HashMap<Integer, Double> subCrowdingDistances = new HashMap<Integer, Double>();
                for (Integer key : paretoFront.keySet()) {
                    subCrowdingDistances.put(key, crowdingDistances.get(key));
                }

                // put data from sorted list to hashmap
                HashMap<Integer, Double> sortedFront = ValueComparator.sortMap(subCrowdingDistances, -1);

                int i = 0;
                for (Integer key : sortedFront.keySet()) {
                    if (i < remainingCapacity) {
                        survivors.add(population.get(key));
                        i++;
                    } else {
                        break;
                    }
                }
                break;
            }
        }

        return survivors;
    }

    public static ArrayList<Individual> survivorSelectionSGA(int N, ArrayList<Individual> population,
            ArrayList<Double> weightedFitness) {

        ArrayList<Individual> survivors = new ArrayList<Individual>(N);

        // Get crowding distances for desired front, and sort them
        HashMap<Integer, Double> fitnesMap = new HashMap<Integer, Double>();
        for (int i = 0; i < population.size(); i++) {
            fitnesMap.put(i, weightedFitness.get(i));
        }

        HashMap<Integer, Double> sortedFitness = ValueComparator.sortMap(fitnesMap, 1);
        Iterator iterator = sortedFitness.keySet().iterator();
        for (int i = 0; i < N; i++) {
            survivors.add(population.get((int) iterator.next()));
        }
        return survivors;
    }

    public static ArrayList<Individual> createOffspring(ArrayList<Individual> parents, int[][] neighborhood, double pC,
            double pM) {
        ArrayList<Individual> offspring = new ArrayList<Individual>(parents.size());
        // Crossover
        for (int i = 0; i < parents.size(); i += 2) {
            Individual parent1 = new Individual();
            Individual parent2 = new Individual();
            parent1.pixelDirection = new ArrayList<Integer>(parents.get(i).pixelDirection); // Shallow copy O(n)
            parent2.pixelDirection = new ArrayList<Integer>(parents.get(i + 1).pixelDirection); // Shallow copy O(n)
            if (Math.random() < pC) {
                // Making parent copies that end up as offspring
                singlePointCrossover(parent1, parent2, offspring);
            } else {
                offspring.add(parent1);
                offspring.add(parent2);
            }
        }
        // Mutation
        for (int i = 0; i < offspring.size(); i++) {
            for (int pixelIndex = 0; pixelIndex < offspring.get(i).pixelDirection.size(); pixelIndex++) {
                if (Math.random() < pM) {
                    int neighboor = -1;
                    int direction = 0;
                    // Lazy dumb solution
                    while (neighboor == -1) {
                        direction = (int) (Math.random() * Utils.PixelDirection.values().length - 1);
                        neighboor = neighborhood[pixelIndex][direction];
                    }
                    offspring.get(i).pixelDirection.set(pixelIndex, direction);
                }
            }
        }

        return offspring;
    }

    public static void singlePointCrossover(Individual parent1, Individual parent2, ArrayList<Individual> offspring) {

        int point = (int) (Math.random() * parent1.pixelDirection.size());
        ArrayList<Integer> offspring1 = new ArrayList<Integer>(parent1.pixelDirection.subList(0, point));
        ArrayList<Integer> offspring2 = new ArrayList<Integer>(parent2.pixelDirection.subList(0, point));
        offspring1.addAll(parent2.pixelDirection.subList(point, parent2.pixelDirection.size()));
        offspring2.addAll(parent1.pixelDirection.subList(point, parent1.pixelDirection.size()));

        parent1.pixelDirection = offspring1;
        parent2.pixelDirection = offspring2;
        offspring.add(parent1);
        offspring.add(parent2);
    }
}
