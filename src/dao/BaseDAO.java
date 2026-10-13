package dao;

import java.util.List;

public interface BaseDAO<T> {
    boolean insert(T entity);
    boolean update(T entity);
    T getById(int id);
    List<T> getAll();
    boolean delete(int id);
}