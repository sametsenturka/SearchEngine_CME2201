public class Article {

    private String id; // IDs are alphanumeric so they become strings
    private String headline;
    private String content;

    // Constructor Method
    public Article(String id, String headline, String content) {
        this.id = id;
        this.headline = headline;
        this.content = content;
    }
    // Getters
    public String getId() { return id; }
    public String getHeadline() { return headline; }
    public String getContent() { return content; }

    @Override
    public String toString() {
        return "Article [ID=" + getId() + ", Headline='" + getHeadline() + "']";
    }
}