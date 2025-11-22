package Data;

import HashMaps.HashMapCustom;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.*;


public class ArticleLoader {

    private static HashMapCustom<String, Article> articleMap;
    private static HashMapCustom<String, HashSet<String>> indexMap;
    private static HashMapCustom<String, Integer> wordFrequencyMap;
    private static HashSet<String> stopWords;
    private static String delimitersRegex;

    public ArticleLoader(int initialCapacity, double loadFactor,
                         HashMapCustom.HashFunctionType hashFunctionType, HashMapCustom.CollisionType collisionType)  {

        this.articleMap = new HashMapCustom<>(initialCapacity, loadFactor, hashFunctionType, collisionType);
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

    public static long loadArticles(String filename) {
        String csvSplitRegex = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
        int linesRead = 0;
        int articleCount = 0;

        try (Scanner fileScanner = new Scanner(new File(filename), "UTF-8")) {
            if (fileScanner.hasNextLine()) fileScanner.nextLine(); // header

            String line = "";
            while (fileScanner.hasNextLine()) {
                line = fileScanner.nextLine();
                linesRead++;
                String[] columns = line.split(csvSplitRegex, -1); // -1 to keep trailing empty cols

                if (columns.length < 8) {
                    System.err.println("Skipped line " + linesRead + ": expected >=8 columns but got " + columns.length);
                    continue;
                }

                String id = stripQuotes(columns[0].trim());
                String firstTopic = columns[3].trim();
                String secondTopic = columns[4].trim();
                String headline = stripQuotes(columns[6].trim()) + " Topics: [" + firstTopic + "], [" + secondTopic + "] ";
                String summary = columns[7].trim();
                String content = columns[10].trim();


                Article article = new Article(id, headline, content, summary);
                articleMap.put(id, article);
                articleCount++;

                if (articleMap.size() % 1000 == 0) {
                    System.out.println("->  %" + (articleMap.size()/280) + " uploaded. (Last ID: " + id + ")");
                }
            }

            System.out.println("Success: " + filename + " uploaded. Total of " + articleCount + " articles added.");
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: " + filename + " can't be reached.");
        }

        return articleMap.getCollisionCount();
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\"\"", "\"");
    }

    public static long indexArticles() {
        System.out.println("---Indexing---");

        long start = System.nanoTime();
        int indexedCount = 0;

        Pattern pattern = Pattern.compile("\\b\\w+\\b");

        String articleId = null;
        Article article = null;
        String content = null;
        Matcher matcher = null;

        for (Map.Entry<String, Article> entry : articleMap.entrySet()) {
            if (entry == null) continue;
            if (entry.getKey() == null) continue;

            articleId = String.valueOf(entry.getKey());
            article = entry.getValue();
            if (article == null) continue;

            content = article.getContent();
            if (content == null || content.isEmpty()) continue;

            matcher = pattern.matcher(content.toLowerCase());

            while (matcher.find()) {
                String word = matcher.group().trim();

                if (word.isEmpty() || stopWords.contains(word)) continue;

                wordFrequencyMap.put(word, wordFrequencyMap.getOrDefault(word, 0) + 1);

                HashSet<String> articleIds = indexMap.get(word);
                if (articleIds == null) {
                    articleIds = new HashSet<>();
                    indexMap.put(word, articleIds);
                }
                articleIds.add(articleId);
                indexedCount++;
            }
        }

        long end = System.nanoTime();
        long millis = (end - start) / 1000000;

        System.out.println("Success: Total of " + indexMap.size() + " words listed in " + (double)(millis/1000) + " seconds.");
        System.out.println("---Indexing Complete---");

        return millis;
    }

    public static long  searchArticles(String searchQuery) {

        long start = System.nanoTime();

        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            System.out.println("No word found for given search query");
            long end = System.nanoTime();
            long millis = (end - start) / 1000000;
            return millis;
        }

        String normalizedQuery = normalizeText(searchQuery);
        if (normalizedQuery.isEmpty()) {
            System.out.println("No word found for given search query");
            long end = System.nanoTime();
            long millis = (end - start) / 1000000;
            return millis;
        }

        String[] queryWords = normalizedQuery.split("\\s+");
        if (queryWords.length == 0) {
            System.out.println("No valid query words found.");
            long end = System.nanoTime();
            long millis = (end - start) / 1000000;
            return millis;
        }

        HashMap<String, Integer> articleScores = new HashMap<>();

        HashSet<String> posting = null;
        for (String term : queryWords) {
            posting = indexMap.get(term);
            if (posting == null) continue;

            for (String articleId : posting) {
                Article article = articleMap.get(articleId);
                if (article == null) continue;

                int score = articleScores.getOrDefault(articleId, 0);

                String normalizedTitle = normalizeText(article.getHeadline());
                String normalizedContent = normalizeText(article.getContent());
                String normalizedSummary = normalizeText(article.getSummary());

                if (normalizedTitle.contains(term)) {
                    score += 19;
                }

                if(normalizedSummary.contains(term)){
                    score += 7;
                }

                if (normalizedContent.contains(term)) {
                    score += 5;
                }

                articleScores.put(articleId, score);
            }
        }

        // Hiç eşleşme yoksa
        if (articleScores.isEmpty()) {
            System.out.println("No articles found for your query.");
            long end = System.nanoTime();
            long millis = (end - start) / 1000000;
            return millis;
        }

        List<Map.Entry<String, Integer>> sortedResults = new ArrayList<>(articleScores.entrySet());
        sortedResults.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        System.out.println("\n--- Search Results for '" + searchQuery + "' ---");
        int shown = 0;

        String relevancy = "";
        for (Map.Entry<String, Integer> entry : sortedResults) {
            String articleId = entry.getKey();
            int score = entry.getValue();
            Article article_results = articleMap.get(articleId);
            if (article_results == null) continue;

            if(score>70){
                relevancy = "HIGHLY Relevant";
            } else if(score>60){
                relevancy = "Very Relevant";
            } else if(score>31){
                relevancy = "Relevant";
            } else{
                relevancy = "Irrelevant";
            }

            System.out.println("[" + relevancy + "][score: " + score + "pts] " + article_results.getHeadline());
            shown++;

            if (shown >= 25) break; // ilk 20 sonucu göster
        }

        long end = System.nanoTime();
        long millis = (end - start) / 1000000;

        System.out.println("\nTotal results: " + sortedResults.size());

        return millis;
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
