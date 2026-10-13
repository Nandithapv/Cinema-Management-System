package views;

import dao.MovieDAO;
import models.Movie;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class AdminMovieDashboardView extends JPanel {

    private static final int COL_NO = 0;
    private static final int COL_TITLE = 1;
    private static final int COL_GENRE = 2;
    private static final int COL_DURATION = 3;
    private static final int COL_LANGUAGE = 4;
    private static final int COL_ID = 5;

    private final MovieDAO dao = new MovieDAO();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"No.", "Title", "Genre", "Duration", "Language", "ID"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    private final JTextField txtTitle = new JTextField(12);
    private final JTextField txtGenre = new JTextField(8);
    private final JTextField txtDuration = new JTextField(5);
    private final JTextField txtLanguage = new JTextField(8);

    public AdminMovieDashboardView() {
        setLayout(new BorderLayout(10, 10));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.removeColumn(table.getColumnModel().getColumn(COL_ID));
        table.getColumnModel().getColumn(COL_NO).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new FlowLayout());
        form.add(new JLabel("Title:"));    form.add(txtTitle);
        form.add(new JLabel("Genre:"));    form.add(txtGenre);
        form.add(new JLabel("Minutes:"));  form.add(txtDuration);
        form.add(new JLabel("Language:")); form.add(txtLanguage);

        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(btnAdd); buttons.add(btnUpdate);
        buttons.add(btnDelete); buttons.add(btnClear);

        JPanel south = new JPanel(new GridLayout(2, 1));
        south.add(form);
        south.add(buttons);
        add(south, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            int r = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && r >= 0) {
                txtTitle.setText(text(model.getValueAt(r, COL_TITLE)));
                txtGenre.setText(text(model.getValueAt(r, COL_GENRE)));
                txtDuration.setText(text(model.getValueAt(r, COL_DURATION)));
                txtLanguage.setText(text(model.getValueAt(r, COL_LANGUAGE)));
            }
        });

        btnAdd.addActionListener(e -> save(false));
        btnUpdate.addActionListener(e -> save(true));
        btnDelete.addActionListener(e -> deleteSelected());
        btnClear.addActionListener(e -> clearForm());

        refresh();
    }

    private String text(Object o) {
        return o == null ? "" : o.toString();
    }

    private void refresh() {
        model.setRowCount(0);
        int n = 1;
        for (Movie m : dao.getAll()) {
            model.addRow(new Object[]{n++, m.getTitle(), m.getGenre(),
                    m.getDuration(), m.getLanguage(), m.getMovieId()});
        }
    }

    private void clearForm() {
        txtTitle.setText(""); txtGenre.setText("");
        txtDuration.setText(""); txtLanguage.setText("");
        table.clearSelection();
    }

    private void save(boolean isUpdate) {
        try {
            Movie m = new Movie();
            if (isUpdate) {
                int r = table.getSelectedRow();
                if (r < 0) {
                    JOptionPane.showMessageDialog(this, "Select a movie in the table first.");
                    return;
                }
                m.setMovieId((int) model.getValueAt(r, COL_ID));
            }
            m.setTitle(txtTitle.getText());
            m.setGenre(txtGenre.getText().trim());
            m.setDuration(Integer.parseInt(txtDuration.getText().trim()));
            m.setLanguage(txtLanguage.getText().trim());

            boolean ok = isUpdate ? dao.update(m) : dao.insert(m);
            if (ok) {
                refresh();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Could not save the movie.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Minutes must be a whole number.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void deleteSelected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a movie in the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete this movie?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        if (dao.delete((int) model.getValueAt(r, COL_ID))) {
            refresh();
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Cannot delete: this movie still has shows scheduled.");
        }
    }
}