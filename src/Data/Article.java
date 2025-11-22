package Data;

public class Article {

    private String id; // IDs are alphanumeric so they become strings
    private String headline;
    private String content;
    private String summary;

    // Constructor Method
    public Article(String id, String headline, String content) {
        this.id = id;
        this.headline = headline;
        this.content = content;
    }

    public Article(String id, String headline, String content, String summary) {
        this.id = id;
        this.headline = headline;
        this.content = content;
        this.summary = summary;
    }

    // Getters
    public String getId() { return id; }
    public String getHeadline() { return headline; }
    public String getContent() { return content; }
    public String getSummary(){ return summary; }

    @Override
    public String toString() {
        return "Data.Article [ID=" + getId() + ", Headline='" + getHeadline() + "']";
    }
}
