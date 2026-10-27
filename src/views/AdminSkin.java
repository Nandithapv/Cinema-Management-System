package views;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Gives the plain Swing admin screens the same black Cineva look as the customer page.
 *
 *   AdminSkin.installDefaults();   // once, before the screens are built (sets colours / rounding)
 *   AdminSkin.apply(panel);        // walks a screen and restyles tables, buttons, fields, labels...
 *
 * apply() can be called as often as you like (it only sets colours, fonts and borders), so call it
 * again after a screen rebuilds part of itself.
 */
final class AdminSkin {

    private static final Color BG = CustomerHomeFrame.BG;
    private static final Color TEXT = CustomerHomeFrame.TEXT;
    private static final Color DIM = CustomerHomeFrame.DIM;
    private static final Color LINE = CustomerHomeFrame.LINE;

    private static final Color FIELD = new Color(14, 14, 18);
    private static final Color BUTTON = new Color(20, 22, 28);
    private static final Color TABLE_BG = new Color(9, 9, 12);
    private static final Color TABLE_GRID = new Color(34, 36, 44);
    private static final Color SELECTION = new Color(52, 56, 68);
    private static final Color LABEL = new Color(190, 194, 202);
    private static final Color FOCUS = new Color(150, 154, 164);

    private AdminSkin() {
    }

    /** Global colours and rounded corners (works with FlatLaf; harmless with other look-and-feels). */
    static void installDefaults() {
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", new Color(16, 16, 20));
        UIManager.put("OptionPane.messageForeground", TEXT);

        UIManager.put("Button.arc", 999);
        UIManager.put("Component.arc", 14);
        UIManager.put("TextComponent.arc", 14);
        UIManager.put("Component.borderColor", LINE);
        UIManager.put("Component.focusedBorderColor", FOCUS);
        UIManager.put("Component.focusColor", FOCUS);
        UIManager.put("Component.focusWidth", 0);

        UIManager.put("Button.background", BUTTON);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.borderColor", new Color(88, 92, 102));
        UIManager.put("Button.hoverBorderColor", new Color(190, 194, 204));

        for (String k : new String[] {"TextField", "PasswordField", "FormattedTextField", "TextArea",
                "ComboBox", "Spinner", "EditorPane"}) {
            UIManager.put(k + ".background", FIELD);
            UIManager.put(k + ".foreground", TEXT);
        }
        UIManager.put("ComboBox.buttonBackground", FIELD);
        UIManager.put("ComboBox.popupBackground", FIELD);
        UIManager.put("PopupMenu.background", FIELD);
        UIManager.put("List.background", FIELD);
        UIManager.put("List.foreground", TEXT);
        UIManager.put("List.selectionBackground", SELECTION);
        UIManager.put("List.selectionForeground", Color.WHITE);

        UIManager.put("Table.background", TABLE_BG);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.gridColor", TABLE_GRID);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background", FIELD);
        UIManager.put("TableHeader.foreground", DIM);
        UIManager.put("TableHeader.bottomSeparatorColor", LINE);

        UIManager.put("ScrollPane.background", BG);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.thumb", new Color(70, 74, 84));
        UIManager.put("ScrollBar.track", BG);
    }

    /** Restyles a screen and everything inside it. */
    static void apply(Container root) {
        if (root == null) {
            return;
        }
        style(root);
        for (Component child : root.getComponents()) {
            if (child instanceof Container) {
                apply((Container) child);
            } else {
                style(child);
            }
        }
    }

    // ---------------------------------------------------------------------

    private static void style(Component c) {
        if (c instanceof JTable) {
            styleTable((JTable) c);
        } else if (c instanceof JTableHeader) {
            styleHeader((JTableHeader) c);
        } else if (c instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) c;
            sp.setBackground(BG);
            sp.setBorder(BorderFactory.createLineBorder(LINE));
            sp.getViewport().setBackground(TABLE_BG);
            sp.setOpaque(false);
        } else if (c instanceof JButton) {
            if (!(c.getParent() instanceof JComboBox) && !(c.getParent() instanceof JSpinner)) {
                styleButton((JButton) c);
            }
        } else if (c instanceof JComboBox) {
            JComboBox<?> cb = (JComboBox<?>) c;
            cb.setBackground(FIELD);
            cb.setForeground(TEXT);
            cb.setFont(CustomerHomeFrame.Fonts.get(false, 13f));
        } else if (c instanceof JTextArea) {
            JTextArea ta = (JTextArea) c;
            ta.setBackground(FIELD);
            ta.setForeground(TEXT);
            ta.setCaretColor(TEXT);
            ta.setFont(CustomerHomeFrame.Fonts.get(false, 13f));
        } else if (c instanceof javax.swing.text.JTextComponent) {
            javax.swing.text.JTextComponent tc = (javax.swing.text.JTextComponent) c;
            tc.setBackground(FIELD);
            tc.setForeground(TEXT);
            tc.setCaretColor(TEXT);
            tc.setFont(CustomerHomeFrame.Fonts.get(false, 13f));
        } else if (c instanceof JSpinner) {
            c.setBackground(FIELD);
            c.setForeground(TEXT);
        } else if (c instanceof JCheckBox || c instanceof JRadioButton) {
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(false);
            b.setForeground(LABEL);
            b.setFont(CustomerHomeFrame.Fonts.get(false, 13f));
        } else if (c instanceof JLabel) {
            JLabel l = (JLabel) c;
            l.setForeground(LABEL);
            boolean bold = l.getFont() != null && l.getFont().isBold();
            float size = l.getFont() != null && l.getFont().getSize2D() > 15f ? l.getFont().getSize2D() : 13f;
            l.setFont(CustomerHomeFrame.Fonts.get(bold, size));
        } else if (c instanceof JList) {
            JList<?> list = (JList<?>) c;
            list.setBackground(FIELD);
            list.setForeground(TEXT);
            list.setSelectionBackground(SELECTION);
            list.setSelectionForeground(Color.WHITE);
        } else if (c instanceof JSeparator) {
            c.setForeground(LINE);
        } else if (c instanceof JPanel) {
            c.setBackground(BG);
        }
    }

    private static void styleButton(JButton b) {
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setBackground(BUTTON);
        b.setForeground(TEXT);
        b.setFont(CustomerHomeFrame.Fonts.get(true, 11.5f));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(6, 18, 6, 18));
    }

    private static void styleTable(JTable t) {
        t.setBackground(TABLE_BG);
        t.setForeground(TEXT);
        t.setGridColor(TABLE_GRID);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setRowHeight(36);
        t.setSelectionBackground(SELECTION);
        t.setSelectionForeground(Color.WHITE);
        t.setFont(CustomerHomeFrame.Fonts.get(false, 13f));
        t.setFillsViewportHeight(true);
        styleHeader(t.getTableHeader());
    }

    private static void styleHeader(JTableHeader h) {
        if (h == null) {
            return;
        }
        h.setBackground(FIELD);
        h.setForeground(DIM);
        h.setFont(CustomerHomeFrame.Fonts.get(true, 11f));
        h.setReorderingAllowed(false);
        h.setBorder(new EmptyBorder(0, 0, 0, 0));
        h.setPreferredSize(new Dimension(Math.max(1, h.getPreferredSize().width), 38));
    }
}