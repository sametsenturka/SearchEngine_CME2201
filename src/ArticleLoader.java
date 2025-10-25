import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Map;

public class ArticleLoader {

    // TEMEL VERİ YAPILARI (HASH MAPLER)
    private HashMap<String, Article> articleMap;
    private HashMap<String, HashSet<String>> indexMap;
    private HashMap<String, Integer> wordFrequencyMap;
    private HashSet<String> stopWords;
    private String delimitersRegex;

    // constructor
    public ArticleLoader() {
        this.articleMap = new HashMap<>();
        this.indexMap = new HashMap<>();
        this.wordFrequencyMap = new HashMap<>();
        this.stopWords = new HashSet<>();
        this.delimitersRegex = "";
    }
    // VERİ YÜKLEME VE ÖN İŞLEME (PREPROCESSING) METOTLARI
    public void loadStopWords(String filename) { // Stop words dosyasını oku
        try {
            Scanner scanner = new Scanner(new File(filename));
            while (scanner.hasNext()) {
                this.stopWords.add(scanner.next().toLowerCase()); //küçük harfe çevirerekk ekle
            }
            scanner.close();
            System.out.println("Başarılı: " + filename + " yüklendi. Toplam " + this.stopWords.size() + " durdurma kelimesi.");
        } catch (FileNotFoundException e) {
            System.err.println("Hata: " + filename + " bulunamadı.");
        }
    }

    public void loadDelimiters(String filename) {// Ayraçları dosyadan oku
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
            this.delimitersRegex = "[" + allDelimiters.toString().replace("[", "").replace("]", "") + "]";
            System.out.println("Başarılı: " + filename + " yüklendi. Regex: " + this.delimitersRegex);
        } catch (FileNotFoundException e) {
            System.err.println("Hata: " + filename + " bulunamadı.");
        }
    }
    public void loadArticles(String filename) {// Makaleleri CSV dosyasından oku
        String csvSplitRegex = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";// CSV ayırıcı regex(virgül, tırnak içindekileri atlar)
        int linesRead = 0;// Okunan satır sayacı(debug için)
        try (Scanner fileScanner = new Scanner(new File(filename))) {
            if (fileScanner.hasNextLine()) {
                fileScanner.nextLine(); // Başlık satırını atla
            }
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                linesRead++;
                String[] columns = line.split(csvSplitRegex);

                if (columns.length < 8) continue;

                try {
                    String id = columns[0].trim();
                    String headline = columns[6].trim();
                    String content = columns[7].trim();

                    Article article = new Article(id, headline, content);
                    this.articleMap.put(id, article);

                    if (articleMap.size() % 1000 == 0) {
                        System.out.println("-> " + articleMap.size() + " makale yüklendi. (Son ID: " + id + ")");
                    }

                } catch (ArrayIndexOutOfBoundsException e) {
                    // Sütun hatasını atla
                }
            }
            System.out.println("Başarılı: " + filename + " yüklendi. Toplam " + articleMap.size() + " makale Article Map'e eklendi.");
        } catch (FileNotFoundException e) {
            System.err.println("Hata: " + filename + " dosyası bulunamadı.");
        }
    }
    // INDEXING METODU
    public void indexArticles() {// Makaleleri dizinleme metodu
        System.out.println("2. ANA DİZİNLEME (INDEXING) BAŞLANGICI");

        for (Map.Entry<String, Article> entry : articleMap.entrySet()) {
            String articleId = String.valueOf(entry.getKey());
            Article article = entry.getValue();

            String content = article.getContent();
            // Makale içeriğini, ayraç regex'ine göre kelimelere böl
            String[] words = content.split(this.delimitersRegex);

            for (String rawWord : words) {
                String word = rawWord.trim().toLowerCase();

                // Stop word veya boş kelimeleri atla
                if (word.isEmpty() || this.stopWords.contains(word)) continue;

                // Kelime Frekans Haritasını güncelle (O(1) sürede)
                this.wordFrequencyMap.put(word, this.wordFrequencyMap.getOrDefault(word, 0) + 1);

                // Index Haritasını güncelle (Kelimenin geçtiği ID'yi kaydet)
                HashSet<String> articleIds = this.indexMap.get(word);

                if (articleIds == null) {
                    articleIds = new HashSet<>();
                    this.indexMap.put(word, articleIds);
                }
                articleIds.add(articleId); // HashSet olduğu için ID'yi sadece bir kez kaydeder
            }
        }

        System.out.println("Başarılı: Toplam " + this.indexMap.size() + " benzersiz kelime dizinlendi.");
        System.out.println("--- 2. ANA DİZİNLEME TAMAMLANDI ---");
    }

    // SEARCH METODU
    public void searchArticles(String searchQuery) {
        String[] rawWords = searchQuery.split(this.delimitersRegex); // Arama sorgusunu ayraçlara göre böl
        HashSet<String> searchTerms = new HashSet<>();// Arama terimleri seti

        // Arama sorgusunu temizle
        for (String rawWord : rawWords) {
            String word = rawWord.trim().toLowerCase();// Küçük harfe çevir ve boşlukları temizle
            if (!word.isEmpty() && !this.stopWords.contains(word)) {// Stop word değilse ekle
                searchTerms.add(word);
            }
        }
        if (searchTerms.isEmpty()) {
            System.out.println("Arama sorgusu için geçerli kelime bulunamadı.");
            return;
        }

        // İlk kelimenin sonuç kümesiyle başla
        HashSet<String> resultSet = null;
        boolean firstTerm = true;

        for (String term : searchTerms) {
            // Index Map'ten ID'leri al (O(1))
            HashSet<String> currentIds = this.indexMap.get(term);

            if (currentIds == null) {
                System.out.println("-> '" + term + "' kelimesi makalelerde bulunamadı. Hiçbir sonuç yok.");
                return;
            }

            if (firstTerm) {
                resultSet = new HashSet<>(currentIds);
                firstTerm = false;
            } else {
                // Diğer kelimeler için KESİŞİM (AND/VE) işlemi yap
                resultSet.retainAll(currentIds);
            }
        }

        // Sonuçları Article Map'ten çek ve göster
        if (resultSet != null && !resultSet.isEmpty()) {
            System.out.println("\n--- ARAMA SONUÇLARI ('" + searchQuery + "') ---");
            System.out.println("Bulunan toplam makale sayısı: " + resultSet.size());

            for (String id : resultSet) {
                Article article = this.articleMap.get(id); // Article Map'ten makaleyi çek
                System.out.println(article); // Article sınıfının toString metodu ile yazdır
            }
        } else {
            System.out.println("\n--- ARAMA SONUÇLARI ('" + searchQuery + "') ---");
            System.out.println("Aradığınız kelimelerin tümünün geçtiği makale bulunamadı.");
        }
    }

}