package dao;

import models.Movie;

public class TestMovieDAO {
    public static void main(String[] args) {
        MovieDAO dao = new MovieDAO();
        dao.insert(new Movie(0, "Leo", "Action", 164, "Tamil"));
        for (Movie m : dao.getAll()) {
            System.out.println(m.getMovieId() + " - " + m);
        }
    }
}