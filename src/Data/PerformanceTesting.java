package SearchEngine;

import Data.ArticleLoader;
import HashMaps.HashMapCustom;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static HashMaps.HashMapCustom.CollisionType.LINEAR_PROBING;
import static HashMaps.HashMapCustom.CollisionType.DOUBLE_HASHING;


import static HashMaps.HashMapCustom.HashFunctionType.SSF;
import static HashMaps.HashMapCustom.HashFunctionType.PAF;


public class PerformanceTesting {
    public static void main(String[] args) throws FileNotFoundException {

        int initialCapacity = 101;
        double loadFactor = 0.8;
        HashMaps.HashMapCustom.HashFunctionType hash = PAF;
        HashMaps.HashMapCustom.CollisionType collision = LINEAR_PROBING;

        String results = PerformanceTestingResults(initialCapacity, loadFactor, hash, collision);

        System.out.println(results);

    }

    public static String PerformanceTestingResults(int initialCapacity, double loadFactor,
                                                   HashMapCustom.HashFunctionType hashFunctionType, HashMapCustom.CollisionType collisionType)
                                            throws FileNotFoundException {

        ArticleLoader articleLoader = new ArticleLoader(initialCapacity, loadFactor, hashFunctionType, collisionType);
        Scanner scanner = new Scanner(System.in);

        articleLoader.loadStopWords("src/Data/stop_words_en.txt");
        articleLoader.loadDelimiters("src/Data/delimiters.txt");
        long collisionCount = articleLoader.loadArticles("src/Data/CNN_Articels.csv");
        long indexTime = articleLoader.indexArticles();


        File file = new File("src/Data/search.txt");
        Scanner scan = new Scanner(file);

        long searchTimeCount=0;
        int wordCount=0;
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            searchTimeCount += articleLoader.searchArticles(line);
            wordCount++;
        }
        double searchTime = searchTimeCount/wordCount;

        articleLoader.searchArticles("src/Data/search.txt");

        String result = "Load Factor (alpha): " + loadFactor + "\n" +
                        "Hash Function: " + hashFunctionType + "\n" +
                        "Collision Handling: " + collisionType + "\n" +
                        "Collision Count: " + collisionCount + "\n" +
                        "Indexing Time: " + indexTime + "ms\n" +
                        "Average Search Time: " + searchTime + "ms";

        return result;
    }
}
