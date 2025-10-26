package Data;

import HashMaps.HashMapArticleSearchOptimised;
import HashMaps.HashMapCustom;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class ArticleLoader {

    private static HashMapCustom<String, Article> articleMap;
    private static HashMapCustom<String, HashSet<String>> indexMap;
    private static HashMapCustom<String, Integer> wordFrequencyMap;
    private static HashSet<String> stopWords;
    private static String delimitersRegex;

    public ArticleLoader() {
        this.articleMap = new HashMapCustom<>();
        this.indexMap = new HashMapCustom<>();
        this.wordFrequencyMap = new HashMapCustom<>();
        this.stopWords = new HashSet<>();
        this.delimitersRegex = " ";
    }

    public static void loadStopWords(String filename) {
        try {
            Scanner scanner = new Scanner(new File(filename));
            while (scanner.hasNext()) {
                stopWords.add(scanner.next().toLowerCase());
            }
            scanner.close();
            System.out.println("Success: " + filename + " uploaded. Total of " + stopWords.size() + " stop words.");
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: " + filename + " can't be reached.");
        }
    }

    public static void loadDelimiters(String filename) {
        StringBuilder allDelimiters = new StringBuilder();
        try {
            Scanner scanner = new Scanner(new File(filename));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();// Satırı oku ve boşlukları temizle

                if (line.isEmpty() || line.startsWith("//")) continue;// YENİ EKLEME: Boş satırları ve yorum satırlarını atla

                // 1. Yorumları sil
                line = line.split("//")[0].trim();

                // 2. Gereksiz Java kod parçalarını sil
                line = line.replace("\"", "")
                        .replace("+", "")
                        .replace(";", "")
                        .replace("String DELIMITERS =", "")
                        // YENİ EKLEME: Kod içindeki olası diğer kalıntıları temizle
                        .replace("String[] splitted = text.split(DELIMITERS)", "")
                        .trim();

                allDelimiters.append(line);
            }
            scanner.close();
            // Regex formatına dönüştürürken fazladan parantezi kaldır
            delimitersRegex = "[" + allDelimiters.toString().replace("[", "").replace("]", "") + "]";
            System.out.println("Success: " + filename + " uploaded. Regex: " + delimitersRegex);
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: " + filename + " can't be reached.");
        }
    }

    public static void loadArticles(String filename) {
        String csvSplitRegex = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
        int linesRead = 0;

        try (Scanner fileScanner = new Scanner(new File(filename), "UTF-8")) {
            if (fileScanner.hasNextLine()) fileScanner.nextLine(); // header

            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                linesRead++;
                String[] columns = line.split(csvSplitRegex, -1); // -1 to keep trailing empty cols

                if (columns.length < 8) {
                    System.err.println("Skipped line " + linesRead + ": expected >=8 columns but got " + columns.length);
                    continue;
                }

                String id = stripQuotes(columns[0].trim());
                String headline = stripQuotes(columns[6].trim());
                String content = stripQuotes(columns[7].trim());

                Article article = new Article(id, headline, content);
                articleMap.put(id, article);

                if (articleMap.size() % 1000 == 0) {

                    System.out.println("->  %" + (articleMap.size()/280) + " uploaded. (Last ID: " + id + ")");
                }
            }

            System.out.println("Success: " + filename + " uploaded. Total of " + articleMap.size() + " articles added.");
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: " + filename + " can't be reached.");
        }
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\"\"", "\"");
    }

    public static void indexArticles() {// Makaleleri dizinleme metodu
        System.out.println("---Indexing---");

        for (Map.Entry<String, Article> entry : articleMap.entrySet()) {

            if (entry == null) continue;
            if (entry.getKey() == null) continue;

            String articleId = String.valueOf(entry.getKey());
            Article article = entry.getValue();
            if (article == null) continue;

            String content = article.getContent();
            String[] words = content.split(" ");

            for (String rawWord : words) {

                rawWord = normalizeText(rawWord);
                rawWord = rawWord.trim();

                String word = rawWord.trim().toLowerCase();

                if (word.isEmpty() || stopWords.contains(word)) continue;

                wordFrequencyMap.put(word, wordFrequencyMap.getOrDefault(word, 0) + 1);

                HashSet<String> articleIds = indexMap.get(word);

                if (articleIds == null) {
                    articleIds = new HashSet<>();
                    indexMap.put(word, articleIds);
                }
                articleIds.add(articleId); // HashSet olduğu için ID'yi sadece bir kez kaydeder
            }
        }


        System.out.println("Success: Total of " + indexMap.size() + " words listed.");
        System.out.println("---Indexing Complete---");
    }

    public static void searchArticles(String searchQuery) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            System.out.println("No word found for given search query");
            return;
        }

        String normalizedQuery = normalizeText(searchQuery);
        if (normalizedQuery.isEmpty()) {
            System.out.println("No word found for given search query");
            return;
        }

        String[] queryWords = normalizedQuery.split("\\s+");
        if (queryWords.length == 0) {
            System.out.println("No valid query words found.");
            return;
        }

        HashMap<String, Integer> articleScores = new HashMap<>();

        for (String term : queryWords) {
            HashSet<String> posting = indexMap.get(term);
            if (posting == null) continue; // kelime yoksa atla

            for (String articleId : posting) {
                Article article = articleMap.get(articleId);
                if (article == null) continue;

                int score = articleScores.getOrDefault(articleId, 0);

                String normalizedTitle = normalizeText(article.getHeadline());
                String normalizedContent = normalizeText(article.getContent());

                if (normalizedTitle.contains(term)) {
                    score += 10;
                }

                if (normalizedContent.contains(term)) {
                    score += 1;
                }

                articleScores.put(articleId, score);
            }
        }

        // Hiç eşleşme yoksa
        if (articleScores.isEmpty()) {
            System.out.println("No articles found for your query.");
            return;
        }

        List<Map.Entry<String, Integer>> sortedResults = new ArrayList<>(articleScores.entrySet());
        sortedResults.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        System.out.println("\n--- Search Results for '" + searchQuery + "' ---");
        int shown = 0;
        for (Map.Entry<String, Integer> entry : sortedResults) {
            String articleId = entry.getKey();
            int score = entry.getValue();
            Article article = articleMap.get(articleId);
            if (article == null) continue;

            System.out.println("[" + score + " pts] " + article.getHeadline());
            shown++;

            if (shown >= 20) break; // ilk 20 sonucu göster
        }

        System.out.println("\nTotal results: " + sortedResults.size());
    }

    public static String normalizeText(String text) {
        if (text == null) return "";

        text = text.toLowerCase();

        // Noktalama işaretlerini boşlukla değiştir
        text = text.replaceAll("[^a-z0-9\\s-]", " ");

        // Fazla boşlukları tek boşluğa indir
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }





}
