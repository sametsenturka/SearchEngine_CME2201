package SearchEngine;

import Data.ArticleLoader;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        ArticleLoader articleLoader = new ArticleLoader();
        Scanner scanner = new Scanner(System.in);

        articleLoader.loadStopWords("src/Data/stop_words_en.txt");
        articleLoader.loadDelimiters("src/Data/delimiters.txt");
        articleLoader.loadArticles("src/Data/CNN_Articels.csv");
        articleLoader.indexArticles();

        while(true){
            System.out.print("SEARCH: ");
            String searchQuery = scanner.nextLine();

            articleLoader.searchArticles(searchQuery);
        }

    }
}
