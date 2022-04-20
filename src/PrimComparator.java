package src;

import java.util.Comparator;
import java.util.HashMap;

class primComparator implements Comparator<Double[]> {

    public int compare(Double[] object1, Double[] object2) {
        if (object1[2] > object2[2])
            return 1;
        else if (object1[2] < object2[2])
            return -1;
        return 0;
    }
}