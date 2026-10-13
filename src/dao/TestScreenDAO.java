package dao;

import models.Screen;

public class TestScreenDAO {
    public static void main(String[] args) {
        ScreenDAO dao = new ScreenDAO();
        dao.insert(new Screen(0, "Screen 2", 0));
        for (Screen s : dao.getAll()) {
            System.out.println(s.getScreenId() + " - " + s);
        }
    }
}