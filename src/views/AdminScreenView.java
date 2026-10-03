package views;

import dao.ScreenDAO;
import models.Screen;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminScreenView extends JPanel {

    private final ScreenDAO dao = new ScreenDAO();
    private List<Screen> screens;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"No.", "Screen Name", "Seats"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField txtName = new JTextField(15);

    public AdminScreenView() {
        setLayout(new BorderLayout(10, 10));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new FlowLayout());
        form.add(new JLabel("Screen name:"));
        form.add(txtName);

        JButton btnAdd = new JButton("Add (creates 60 seats)");
        JButton btnRename = new JButton("Rename");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(btnAdd); buttons.add(btnRename);
        buttons.add(btnDelete); buttons.add(btnClear);

        JPanel south = new JPanel(new GridLayout(2, 1));
        south.add(form);
        south.add(buttons);
        add(south, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            int r = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && r >= 0) {
                txtName.setText(screens.get(r).getScreenName());
            }
        });

        btnAdd.addActionListener(e -> save(false));
        btnRename.addActionListener(e -> save(true));
        btnDelete.addActionListener(e -> deleteSelected());
        btnClear.addActionListener(e -> clearForm());

        refresh();
    }

    private void refresh() {
        screens = dao.getAll();
        model.setRowCount(0);
        int n = 1;
        for (Screen s : screens) {
            model.addRow(new Object[]{n++, s.getScreenName(), s.getCapacity()});
        }
    }

    private void clearForm() {
        txtName.setText("");
        table.clearSelection();
    }

    private void save(boolean isUpdate) {
        try {
            Screen s = new Screen();
            if (isUpdate) {
                int r = table.getSelectedRow();
                if (r < 0) {
                    JOptionPane.showMessageDialog(this, "Select a screen in the table first.");
                    return;
                }
                s.setScreenId(screens.get(r).getScreenId());
            }
            s.setScreenName(txtName.getText());

            boolean ok = isUpdate ? dao.update(s) : dao.insert(s);
            if (ok) {
                refresh();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Could not save the screen.");
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void deleteSelected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a screen in the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete this screen and its seats?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        if (dao.delete(screens.get(r).getScreenId())) {
            refresh();
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Cannot delete: this screen has shows or bookings.");
        }
    }
}