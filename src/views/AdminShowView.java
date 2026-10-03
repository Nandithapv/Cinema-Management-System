package views;

import dao.MovieDAO;
import dao.ScreenDAO;
import dao.ShowDAO;
import models.Movie;
import models.Screen;
import models.Show;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.util.List;

public class AdminShowView extends JPanel {

    private final ShowDAO dao = new ShowDAO();
    private List<Show> shows;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"No.", "Movie", "Screen", "Date", "Time", "Price"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    private final JComboBox<Movie> cmbMovie = new JComboBox<>();
    private final JComboBox<Screen> cmbScreen = new JComboBox<>();
    private final JTextField txtDate = new JTextField(8);
    private final JTextField txtTime = new JTextField(5);
    private final JTextField txtPrice = new JTextField(6);

    public AdminShowView() {
        setLayout(new BorderLayout(10, 10));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        for (Movie m : new MovieDAO().getAll()) cmbMovie.addItem(m);
        for (Screen s : new ScreenDAO().getAll()) cmbScreen.addItem(s);

        JPanel form = new JPanel(new FlowLayout());
        form.add(new JLabel("Movie:"));  form.add(cmbMovie);
        form.add(new JLabel("Screen:")); form.add(cmbScreen);
        form.add(new JLabel("Date (yyyy-mm-dd):")); form.add(txtDate);
        form.add(new JLabel("Time (HH:mm):"));      form.add(txtTime);
        form.add(new JLabel("Price:"));  form.add(txtPrice);

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
            if (!e.getValueIsAdjusting() && r >= 0) fillForm(shows.get(r));
        });

        btnAdd.addActionListener(e -> save(false));
        btnUpdate.addActionListener(e -> save(true));
        btnDelete.addActionListener(e -> deleteSelected());
        btnClear.addActionListener(e -> clearForm());

        refresh();
    }
 // Called by the dashboard each time the Shows tab is opened
    public void reloadChoices() {
        cmbMovie.removeAllItems();
        cmbScreen.removeAllItems();
        for (Movie m : new MovieDAO().getAll()) cmbMovie.addItem(m);
        for (Screen s : new ScreenDAO().getAll()) cmbScreen.addItem(s);
        refresh();
    }

    private void refresh() {
        shows = dao.getAll();
        model.setRowCount(0);
        int n = 1;
        for (Show s : shows) {
            model.addRow(new Object[]{n++, s.getMovieTitle(), s.getScreenName(),
                    s.getShowDate(), s.getShowTime().toString().substring(0, 5),
                    s.getTicketPrice()});
        }
    }

    private void fillForm(Show s) {
        for (int i = 0; i < cmbMovie.getItemCount(); i++)
            if (cmbMovie.getItemAt(i).getMovieId() == s.getMovieId()) cmbMovie.setSelectedIndex(i);
        for (int i = 0; i < cmbScreen.getItemCount(); i++)
            if (cmbScreen.getItemAt(i).getScreenId() == s.getScreenId()) cmbScreen.setSelectedIndex(i);
        txtDate.setText(s.getShowDate().toString());
        txtTime.setText(s.getShowTime().toString().substring(0, 5));
        txtPrice.setText(s.getTicketPrice().toPlainString());
    }

    private void clearForm() {
        txtDate.setText(""); txtTime.setText(""); txtPrice.setText("");
        table.clearSelection();
    }

    private void save(boolean isUpdate) {
        try {
            Show s = new Show();
            if (isUpdate) {
                int r = table.getSelectedRow();
                if (r < 0) {
                    JOptionPane.showMessageDialog(this, "Select a show in the table first.");
                    return;
                }
                s.setShowId(shows.get(r).getShowId());
            }
            Movie m = (Movie) cmbMovie.getSelectedItem();
            Screen sc = (Screen) cmbScreen.getSelectedItem();
            if (m == null || sc == null) {
                JOptionPane.showMessageDialog(this, "Add a movie and a screen first.");
                return;
            }
            s.setMovieId(m.getMovieId());
            s.setScreenId(sc.getScreenId());
            s.setShowDate(Date.valueOf(txtDate.getText().trim()));
            s.setShowTime(Time.valueOf(txtTime.getText().trim() + ":00"));
            s.setTicketPrice(new BigDecimal(txtPrice.getText().trim()));

            boolean ok = isUpdate ? dao.update(s) : dao.insert(s);
            if (ok) {
                refresh();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Could not save. That screen may already have a show at this date and time.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price must be a number, like 200 or 150.50.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    "Check the format. Date: 2026-12-25, Time: 18:30, and price must be positive.");
        }
    }

    private void deleteSelected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a show in the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete this show?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        if (dao.delete(shows.get(r).getShowId())) {
            refresh();
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "Cannot delete: this show has bookings.");
        }
    }
}