package src;

// https://www.javatpoint.com/prims-algorithm-java

//import required classes and packages  
import java.lang.*;   
import java.util.*;   
import java.io.*;   
import java.awt.image.BufferedImage;

  
//creating MinimumSpanningTreeExample class to implement Prim's algorithm in Java     
class MST {   
    // Define the count of vertices available in the graph  
    private int countOfVertices; 
    
    public MST(BufferedImage image){
        this.countOfVertices = image.getWidth() * image.getHeight();
    }
    
    // create findMinKeyVertex() method for finding the vertex v that has minimum key-value and that is not added MST yet  
    int findMinKeyVertex(double keys[], Boolean setOfMST[])   
    {   
        // Initialize min value and its index  
        int minimum_index = -1;   
        double minimum_value = Integer.MAX_VALUE;  
          
        //iterate over all vertices to find minimum key-value vertex  
        for (int vertex = 0; vertex < countOfVertices; vertex++)   
            if (setOfMST[vertex] == false && keys[vertex] < minimum_value) {   
                minimum_value = keys[vertex];   
                minimum_index = vertex;   
            }   
    
        return minimum_index;   
    }   
    
    // create showMinimumSpanningTree for printing the constructed MST stored in mstArray[]   
    void showMinimumSpanningTree(int mstArray[], double rgbDistance[][], int[][] neighborhood)   
    {   
        System.out.println("Edge \t\t Weight");   
        for (int j = 1; j < countOfVertices; j++){
            int neighboor = -1;
            for (int i = 0; i < 8; i++) {
                if(neighborhood[j][i] == mstArray[j]){
                    neighboor = i;
                }
            }
            System.out.println(mstArray[j] + " <-> " + j + "\t \t" + rgbDistance[j][neighboor]);   
        }
    }   
    
    // create designMST() method for constructing and printing the MST. The graphArray[][] is an adjacency matrix that defines the graph for MST.  
    int[] designMST(int[][] neighborhood, double[][] rgbDistance)   
    {   
        // create array of size total number of vertices, i.e., countOfVertices for storing the MST  
        int mstArray[] = new int[countOfVertices];   
    
        // create keys[] array for selecting an edge having minimum weight in cut   
        double keys[] = new double[countOfVertices];   
    
        // create setOfMST array of type boolean for representing the set of vertices included in MST   
        Boolean setOfMST[] = new Boolean[countOfVertices];   
    
        // set the value of the keys to infinite   
        for (int j = 0; j < countOfVertices; j++) {   
            keys[j] = Integer.MAX_VALUE;   
            setOfMST[j] = false;   
        }   
    
        // set value 0 to the 1st vertex because first vertes always include in MST.   
        keys[0] = 0; // it select as first vertex   
        mstArray[0] = -1; // set first value of mstArray to -1 to make it root of MST   
    
        // The vertices in the MST will be equal to the countOfVertices   
        for (int i = 0; i < countOfVertices - 1; i++) {   
            // select the vertex having minimum key and that is not added in the MST yet from the set of vertices   
            int edge = findMinKeyVertex(keys, setOfMST);   
    
            // Add the selected minimum key vertex to the setOfMST   
            setOfMST[edge] = true;   
    
            // change the key value and the parent index of all the adjacent vertices of the selected vertex. The vertices that are not yet included in Minimum Spanning Tree are only considered.   
            for (int neighboor = 0; neighboor < 8; neighboor++){ 
                int vertex = neighborhood[edge][neighboor];
                // The value of the graphArray[edge][vertex] is non zero only for adjacent vertices of m setOfMST[vertex] is false for vertices not yet included in Minimum Spanning Tree   
                // when the value of the graphArray[edge][vertex] is smaller than key[vertex], we update the key  
                if (vertex != -1 && setOfMST[vertex] == false && rgbDistance[edge][neighboor] < keys[vertex]) {   
                    mstArray[vertex] = edge;   
                    keys[vertex] = rgbDistance[edge][neighboor];   
                }   
            }  
        } 
    
        // print the constructed Minimum Spanning Tree   
        // showMinimumSpanningTree(mstArray, rgbDistance, neighborhood);   
        return mstArray;
    }   
    //main() method start  
    // public static void main(String[] args)   
    // {   
          
    //     MST mst = new MST();   
    //     double graphArray[][] = new double[][]{{ 0, 4, 0, 0, 0, 0, 0, 8, 0 },   
    //                 { 4, 0, 8, 0, 0, 0, 0, 11, 0 },   
    //                 { 0, 8, 0, 7, 0, 4, 0, 0, 2 },   
    //                 { 0, 0, 7, 0, 9, 14, 0, 0, 0 },   
    //                 { 0, 0, 0, 9, 0, 10, 0, 0, 0 },  
    //                 { 0, 0, 4, 14, 10, 0, 2, 0, 0 },  
    //                 { 0, 0, 0, 0, 0, 2, 0, 1, 6 },  
    //                 { 8, 11, 0, 0, 0, 0, 1, 0, 7 },  
    //                 { 0, 0, 2, 0, 0, 0, 6, 7, 0 }};   
    
    //     // Print the Minimum Spanning Tree solution   
    //     mst.designMST(graphArray);   
    // }   
} 