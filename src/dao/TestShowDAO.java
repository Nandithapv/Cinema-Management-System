package dao;

import models.Movie;
import models.Show;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;

public class TestShowDAO {
    public static void main(String[] args) {
        ShowDAO dao = new ShowDAO();
        Movie m = new MovieDAO().getAll().get(0);

        Show s = new Show(0, m.getMovieId(), 1,
            Date.valueOf("2026-12-25"), Time.valueOf("18:30:00"), new BigDecimal("200.00"));

        System.out.println("First insert: " + dao.insert(s));
        System.out.println("Same slot again (should be false): " + dao.insert(s));

        for (Show x : dao.getAll()) {
            System.out.println(x.getShowId() + " - " + x);
        }
    }
}