package SearchEngine;

import Data.ArticleLoader;
import java.util.Scanner;

public class SearchEngine {
    public static void main(String[] args) {

        ArticleLoader articleLoader = new ArticleLoader();

        articleLoader.loadStopWords("src/Data/stop_words_en.txt");
        articleLoader.loadDelimiters("src/Data/delimiters.txt");
        articleLoader.loadArticles("src/Data/CNN_Articels.csv");
        articleLoader.indexArticles();

        articleLoader.searchArticles("[BURAYA ARAMANI YAZ]");

    }
}