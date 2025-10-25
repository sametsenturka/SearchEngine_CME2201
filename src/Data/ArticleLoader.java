package Data;

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
        this.delimitersRegex = "";
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
                    System.out.println("-> " + articleMap.size() + " uploaded. (Last ID: " + id + ")");
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

        for (Entry<String, Article> entry : articleMap.entrySet()) {

            if(entry!=null){
                String articleId = String.valueOf(entry.getKey());
                Article article = entry.getValue();

                String content = article.getContent();
                // Makale içeriğini, ayraç regex'ine göre kelimelere böl
                String[] words = content.split(delimitersRegex);

                for (String rawWord : words) {
                    String word = rawWord.trim().toLowerCase();

                    // Stop word veya boş kelimeleri atla
                    if (word.isEmpty() || stopWords.contains(word)) continue;

                    // Kelime Frekans Haritasını güncelle (O(1) sürede)
                    wordFrequencyMap.put(word, wordFrequencyMap.getOrDefault(word, 0) + 1);

                    // Index Haritasını güncelle (Kelimenin geçtiği ID'yi kaydet)
                    HashSet<String> articleIds = indexMap.get(word);

                    if (articleIds == null) {
                        articleIds = new HashSet<>();
                        indexMap.put(word, articleIds);
                    }
                    articleIds.add(articleId); // HashSet olduğu için ID'yi sadece bir kez kaydeder
                }
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

        // Güvenli split regex
        String splitRegex = (delimitersRegex == null || delimitersRegex.isEmpty()) ? "\\s+" : delimitersRegex;

        // Normalize query for phrase check (remove delimiters -> single space, lower-case, trim)
        String normalizedQuery = normalizeTextForSearch(searchQuery, splitRegex);
        if (normalizedQuery.isEmpty()) {
            System.out.println("No word found for given search query");
            return;
        }

        // Tokenize query into words for candidate reduction (unique words)
        String[] rawTokens = normalizedQuery.split("\\s+");
        LinkedHashSet<String> queryTerms = new LinkedHashSet<>();
        for (String t : rawTokens) {
            if (t.isEmpty()) continue;
            if (stopWords != null && stopWords.contains(t)) continue; // stop word filter
            queryTerms.add(t);
        }

        if (queryTerms.isEmpty()) {
            System.out.println("No word found for given search query");
            return;
        }

        // Build posting lists for each term and short-circuit if any term missing
        ArrayList<HashSet<String>> postingLists = new ArrayList<>(queryTerms.size());
        for (String term : queryTerms) {
            HashSet<String> posting = indexMap.get(term);
            if (posting == null || posting.isEmpty()) {
                System.out.println("-> Word '" + term + "' is not found. No Results.");
                return;
            }
            postingLists.add(posting);
        }

        // Sort by size ascending to optimize intersection
        postingLists.sort((a, b) -> Integer.compare(a.size(), b.size()));

        // Intersect posting lists to get candidate articles (IDs)
        HashSet<String> candidates = new HashSet<>(postingLists.get(0));
        for (int i = 1; i < postingLists.size(); i++) {
            candidates.retainAll(postingLists.get(i));
            if (candidates.isEmpty()) break;
        }

        // If no candidates -> no results
        System.out.println("\n--- Search Results ('" + searchQuery + "') ---");
        if (candidates.isEmpty()) {
            System.out.println("No article is found containing your search.");
            return;
        }

        // Now do exact phrase check on normalized content of each candidate
        final int MAX_SHOW = 200;
        int shown = 0;
        for (String id : candidates) {
            Article article = articleMap.get(id);
            if (article == null) continue;

            // Normalize article content for phrase checking
            String normalizedContent = normalizeTextForSearch(article.getContent(), splitRegex);

            // Use contains on normalized strings (both are lower-case, delimiters normalized)
            if (normalizedContent.contains(normalizedQuery)) {
                System.out.println(article); // veya daha seçici çıktı
                shown++;
                if (shown >= MAX_SHOW) break;
            }
        }

        if (shown == 0) {
            System.out.println("No article is found containing your search.");
        } else if (shown < candidates.size()) {
            System.out.println("... (" + (candidates.size() - shown) + " more candidate articles contain the words but are not shown)");
        }
    }

    // Yardımcı: metni arama için normalize eder
    private static String normalizeTextForSearch(String text, String splitRegex) {
        if (text == null) return "";
        // Replace delimiters with single space, collapse multiple spaces, toLowerCase
        // splitRegex is already a character-class style regex like "[,.;:...]" or default "\\s+"
        String interim;
        try {
            interim = text.replaceAll(splitRegex, " ");
        } catch (Exception e) {
            // splitRegex bozuksa fallback
            interim = text.replaceAll("\\s+", " ");
        }
        // collapse multiple spaces and trim, toLowerCase
        return interim.replaceAll("\\s+", " ").trim().toLowerCase();
    }



}