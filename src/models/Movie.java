package models;

public class Movie {
    private int movieId;
    private String title;
    private String genre;
    private int duration;
    private String language;

    public Movie() {}

    public Movie(int movieId, String title, String genre, int duration, String language) {
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.language = language;
    }

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        this.title = title.trim();
    }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        this.duration = duration;
    }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    @Override
    public String toString() {
        return title + " (" + genre + ", " + duration + " min, " + language + ")";
    }
}