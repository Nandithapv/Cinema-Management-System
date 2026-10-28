package models;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;

public class Show {
    private int showId;
    private int movieId;
    private int screenId;
    private Date showDate;
    private Time showTime;
    private BigDecimal ticketPrice;

    // Extra fields filled in when listing shows (not stored in the shows table)
    private String movieTitle;
    private String screenName;

    public Show() {}

    public Show(int showId, int movieId, int screenId,
                Date showDate, Time showTime, BigDecimal ticketPrice) {
        this.showId = showId;
        this.movieId = movieId;
        this.screenId = screenId;
        this.showDate = showDate;
        this.showTime = showTime;
        this.ticketPrice = ticketPrice;
    }

    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }

    public int getScreenId() { return screenId; }
    public void setScreenId(int screenId) { this.screenId = screenId; }

    public Date getShowDate() { return showDate; }
    public void setShowDate(Date showDate) { this.showDate = showDate; }

    public Time getShowTime() { return showTime; }
    public void setShowTime(Time showTime) { this.showTime = showTime; }

    public BigDecimal getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(BigDecimal ticketPrice) {
        if (ticketPrice == null || ticketPrice.signum() <= 0) {
            throw new IllegalArgumentException("Ticket price must be positive");
        }
        this.ticketPrice = ticketPrice;
    }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    @Override
    public String toString() {
        return movieTitle + " | " + screenName + " | " + showDate + " " + showTime + " | Rs." + ticketPrice;
    }
}