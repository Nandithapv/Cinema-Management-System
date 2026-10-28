package views;

import dao.MovieDAO;
import models.Movie;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Movies Catalog tab of the admin portal.
 *
 * Same look as the customer page: black page, a small form with pill buttons, then one strip of
 * posters. Hover a poster and it opens up; click it to manage it (the form fills in, and
 * Update / Delete act on it). Add / Update / Delete / Clear talk to MovieDAO exactly as before.
 *
 * Posters are found the same way as on the customer page (CustomerHomeFrame.PosterStrip.readPoster).
 */
public class AdminMovieDashboardView extends JPanel {

    private static final Color BG = CustomerHomeFrame.BG;
    private static final Color TEXT = CustomerHomeFrame.TEXT;
    private static final Color DIM = CustomerHomeFrame.DIM;
    private static final Color LINE = CustomerHomeFrame.LINE;

    private final MovieDAO dao = new MovieDAO();
    private final RoundField fTitle = new RoundField(190);
    private final RoundField fGenre = new RoundField(130);
    private final RoundField fMinutes = new RoundField(80);
    private final RoundField fLanguage = new RoundField(130);
    private final Strip strip;
    private Movie selected;

    public AdminMovieDashboardView() {
        setLayout(new BorderLayout());
        setBackground(BG);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(26, 0, 0, 0));

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        fields.setOpaque(false);
        fields.add(label("Title:"));
        fields.add(fTitle);
        fields.add(Box.createHorizontalStrut(4));
        fields.add(label("Genre:"));
        fields.add(fGenre);
        fields.add(Box.createHorizontalStrut(4));
        fields.add(label("Minutes:"));
        fields.add(fMinutes);
        fields.add(Box.createHorizontalStrut(4));
        fields.add(label("Language:"));
        fields.add(fLanguage);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(14, 0, 0, 0));
        buttons.add(new PillButton("Add", this::onAdd, false));
        buttons.add(new PillButton("Update", this::onUpdate, false));
        buttons.add(new PillButton("Delete", this::onDelete, true));
        buttons.add(new PillButton("Clear", this::onClear, false));

        form.add(fields);
        form.add(buttons);
        add(form, BorderLayout.NORTH);

        strip = new Strip(this::fillForm);
        JPanel stripWrap = new JPanel(new BorderLayout());
        stripWrap.setOpaque(false);
        stripWrap.setBorder(new EmptyBorder(8, 0, 0, 0));
        stripWrap.add(strip, BorderLayout.CENTER);
        add(stripWrap, BorderLayout.CENTER);

        reload(-1);
    }

    private static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(CustomerHomeFrame.Fonts.get(false, 13f));
        l.setForeground(new Color(190, 194, 202));
        return l;
    }

    // =====================================================================
    //  Data
    // =====================================================================
    private void reload(int selectId) {
        List<Movie> list = new ArrayList<>();
        try {
            List<Movie> got = dao.getAll();
            if (got != null) {
                list.addAll(got);
            }
        } catch (Throwable t) {
            System.err.println("[AdminMovieDashboardView] could not load movies: " + t.getMessage());
        }
        strip.setMovies(list, selectId);
        selected = null;
        for (Movie m : list) {
            if (m.getMovieId() == selectId) {
                selected = m;
            }
        }
        if (selected == null) {
            clearFields();
        }
    }

    private void fillForm(Movie m) {
        selected = m;
        if (m == null) {
            clearFields();
            return;
        }
        fTitle.setText(CustomerHomeFrame.nz(m.getTitle()));
        fGenre.setText(CustomerHomeFrame.nz(m.getGenre()));
        fMinutes.setText(String.valueOf(m.getDuration()));
        fLanguage.setText(CustomerHomeFrame.nz(m.getLanguage()));
    }

    private void clearFields() {
        fTitle.setText("");
        fGenre.setText("");
        fMinutes.setText("");
        fLanguage.setText("");
    }

    /** Builds a Movie from the form; throws IllegalArgumentException with a friendly message. */
    private Movie readForm(Movie base) {
        int mins;
        try {
            mins = Integer.parseInt(fMinutes.getText().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Minutes must be a whole number, e.g. 148.");
        }
        Movie m = new Movie();
        if (base != null) {
            m.setMovieId(base.getMovieId());
            m.setPosterPath(base.getPosterPath());
            m.setActive(base.isActive());
        }
        m.setTitle(fTitle.getText());          // rejects an empty title
        m.setDuration(mins);                   // rejects zero / negative
        m.setGenre(fGenre.getText().trim());
        m.setLanguage(fLanguage.getText().trim());
        return m;
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Movies", JOptionPane.WARNING_MESSAGE);
    }

    private void onAdd() {
        Movie m;
        try {
            m = readForm(null);
        } catch (IllegalArgumentException ex) {
            warn(ex.getMessage());
            return;
        }
        if (!dao.insert(m)) {
            warn("Could not add the movie. Please try again.");
            return;
        }
        reload(-1);
        for (Movie x : strip.movies()) {
            if (x.getTitle() != null && x.getTitle().equalsIgnoreCase(m.getTitle())) {
                strip.select(x);
                fillForm(x);
            }
        }
    }

    private void onUpdate() {
        if (selected == null) {
            warn("Click a poster first, then change the details and press Update.");
            return;
        }
        Movie m;
        try {
            m = readForm(selected);
        } catch (IllegalArgumentException ex) {
            warn(ex.getMessage());
            return;
        }
        if (!dao.update(m)) {
            warn("Could not update the movie. Please try again.");
            return;
        }
        int id = m.getMovieId();
        reload(id);
        for (Movie x : strip.movies()) {
            if (x.getMovieId() == id) {
                strip.select(x);
                fillForm(x);
            }
        }
    }

    private void onDelete() {
        if (selected == null) {
            warn("Click a poster first, then press Delete.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete \"" + selected.getTitle() + "\" from the catalog?", "Delete movie",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        if (!dao.delete(selected.getMovieId())) {
            warn("This movie can't be deleted because it still has shows scheduled.\n"
                    + "Remove its shows first.");
            return;
        }
        reload(-1);
    }

    private void onClear() {
        strip.select(null);
        clearFields();
        selected = null;
    }

    // =====================================================================
    //  Form widgets
    // =====================================================================
    static final class RoundField extends JTextField {
        private boolean focused;

        RoundField(int width) {
            setOpaque(false);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setFont(CustomerHomeFrame.Fonts.get(false, 13f));
            setForeground(TEXT);
            setCaretColor(TEXT);
            setPreferredSize(new Dimension(width, 34));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(14, 14, 18));
            g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 12, 12));
            g.setColor(focused ? new Color(150, 154, 164) : LINE);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 12, 12));
            g.dispose();
            super.paintComponent(g0);
        }
    }

    static final class PillButton extends JComponent {
        private final String text;
        private final Runnable action;
        private final boolean danger;
        private boolean hover;
        private boolean down;

        PillButton(String text, Runnable action, boolean danger) {
            this.text = text.toUpperCase();
            this.action = action;
            this.danger = danger;
            FontMetrics fm = getFontMetrics(CustomerHomeFrame.Fonts.get(true, 11f));
            setPreferredSize(new Dimension(Math.round(CustomerHomeFrame.trackedWidth(fm, this.text, 1.4f)) + 44, 32));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; down = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { down = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) {
                    boolean fire = down && contains(e.getPoint());
                    down = false;
                    repaint();
                    if (fire) {
                        action.run();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            int w = getWidth();
            int h = getHeight();
            Color edge = hover ? (danger ? new Color(235, 100, 110) : new Color(190, 194, 204)) : new Color(88, 92, 102);
            if (down) {
                g.setColor(new Color(255, 255, 255, 22));
                g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, h, h));
            }
            g.setColor(edge);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));
            g.setFont(CustomerHomeFrame.Fonts.get(true, 11f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(hover ? (danger ? new Color(255, 140, 148) : Color.WHITE) : TEXT);
            float tw = CustomerHomeFrame.trackedWidth(fm, text, 1.4f);
            CustomerHomeFrame.drawTracked(g, text, (w - tw) / 2f, (h + fm.getAscent() - fm.getDescent()) / 2f, 1.4f);
            g.dispose();
        }
    }

    // =====================================================================
    //  The poster strip
    // =====================================================================
    static final class Strip extends JComponent {
        private static final int SIDE = 28;
        private static final int NAMES_H = 78;
        private static final int BOTTOM_GAP = 18;
        private static final int MIN_W = 96;
        private static final float EXPAND = 3.2f;
        private static final float MAX_LENS_SPEED = 7f;   // posters per second the lens may travel

        private final Consumer<Movie> onSelect;
        private final javax.swing.Timer timer;
        private long lastNanos = System.nanoTime();

        private List<Movie> movies = new ArrayList<>();
        private final Map<Integer, BufferedImage> posters = new HashMap<>();
        private final Set<Integer> requested = new HashSet<>();

        private int sel = -1;
        private float focus = -1f;
        private float pan = 0f;
        private float[] hot = new float[0];
        private float[] weights = new float[0];
        private float[] slotX = new float[0];
        private float[] slotW = new float[0];
        private int autoIdx = 0;
        private float autoTimer = 0f;
        private boolean playing = true;
        private float disc = 0f;

        private boolean mouseIn = false;
        private int mouseX = -1;
        private int mouseY = -1;
        private int hoverIdx = -1;

        private int viewX;
        private int viewW;
        private int stripY;
        private int stripH;

        Strip(Consumer<Movie> onSelect) {
            this.onSelect = onSelect;
            setOpaque(true);
            setBackground(BG);

            timer = new javax.swing.Timer(16, e -> {
                long now = System.nanoTime();
                float dt = Math.min(0.05f, (now - lastNanos) / 1e9f);
                lastNanos = now;
                step(dt);
                repaint();
            });

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { track(e); }
                @Override public void mouseDragged(MouseEvent e) { track(e); }
                @Override public void mouseExited(MouseEvent e) { mouseIn = false; hoverIdx = -1; mouseX = -1; mouseY = -1; }
                @Override public void mousePressed(MouseEvent e) { click(e); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        List<Movie> movies() {
            return movies;
        }

        void setMovies(List<Movie> list, int selectId) {
            movies = new ArrayList<>(list);
            int n = movies.size();
            hot = new float[n];
            weights = new float[n];
            slotX = new float[n];
            slotW = new float[n];
            sel = -1;
            for (int i = 0; i < n; i++) {
                if (movies.get(i).getMovieId() == selectId) {
                    sel = i;
                }
            }
            autoIdx = Math.max(0, sel);
            autoTimer = 0f;
            focus = -1f;
            pan = 0f;
            if (sel >= 0) {
                playing = false;
            }
            loadPosters();
            repaint();
        }

        void select(Movie m) {
            sel = movies.indexOf(m);
            if (sel >= 0) {
                autoIdx = sel;
                playing = false;
            }
            repaint();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            lastNanos = System.nanoTime();
            timer.start();
        }

        @Override
        public void removeNotify() {
            timer.stop();
            super.removeNotify();
        }

        private void loadPosters() {
            for (Movie m : movies) {
                final Movie movie = m;
                final int id = movie.getMovieId();
                if (!requested.add(id)) {
                    continue;
                }
                new SwingWorker<BufferedImage, Void>() {
                    @Override
                    protected BufferedImage doInBackground() {
                        return CustomerHomeFrame.PosterStrip.readPoster(movie);
                    }

                    @Override
                    protected void done() {
                        try {
                            BufferedImage img = get();
                            if (img != null) {
                                posters.put(id, img);
                                repaint();
                            }
                        } catch (Exception ignored) {
                            // fallback artwork is drawn instead
                        }
                    }
                }.execute();
            }
        }

        // ---------- geometry & mouse ----------
        private void computeGeometry() {
            int w = getWidth();
            int h = getHeight();
            viewX = SIDE;
            viewW = Math.max(100, w - 2 * SIDE);
            stripY = NAMES_H;
            stripH = Math.max(160, h - NAMES_H - BOTTOM_GAP);
        }

        private float ctrlCy() { return stripY + stripH - 46; }
        private float prevCx() { return viewX + viewW / 2f - 56; }
        private float playCx() { return viewX + viewW / 2f; }
        private float nextCx() { return viewX + viewW / 2f + 56; }

        private boolean inCircle(float px, float py, float cx, float cy, float r) {
            float dx = px - cx;
            float dy = py - cy;
            return dx * dx + dy * dy <= r * r;
        }

        private int controlAt(int x, int y) {
            if (inCircle(x, y, prevCx(), ctrlCy(), 18)) return 0;
            if (inCircle(x, y, playCx(), ctrlCy(), 26)) return 1;
            if (inCircle(x, y, nextCx(), ctrlCy(), 18)) return 2;
            return -1;
        }

        private int slotAt(int x) {
            for (int i = 0; i < slotX.length; i++) {
                if (x >= slotX[i] && x < slotX[i] + slotW[i]) {
                    return i;
                }
            }
            return -1;
        }

        private void track(MouseEvent e) {
            computeGeometry();
            mouseX = e.getX();
            mouseY = e.getY();
            mouseIn = mouseX >= viewX && mouseX <= viewX + viewW && mouseY >= 0 && mouseY <= stripY + stripH;
            int idx = mouseIn ? slotAt(mouseX) : -1;
            boolean hand = controlAt(mouseX, mouseY) >= 0 || (idx >= 0 && mouseY >= stripY);
            setCursor(hand ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        }

        private void choose(int idx) {
            if (idx < 0 || idx >= movies.size()) {
                return;
            }
            sel = idx;
            autoIdx = idx;
            playing = false;
            onSelect.accept(movies.get(idx));
        }

        private void click(MouseEvent e) {
            computeGeometry();
            int n = movies.size();
            int c = controlAt(e.getX(), e.getY());
            if (c == 1) {
                playing = !playing;
                autoTimer = 0f;
                return;
            }
            if ((c == 0 || c == 2) && n > 0) {
                int dir = c == 0 ? -1 : 1;
                int from = sel >= 0 ? sel : (dir > 0 ? -1 : 0);
                choose((from + dir + n) % n);
                return;
            }
            if (e.getY() >= stripY && e.getY() <= stripY + stripH) {
                int idx = slotAt(e.getX());
                if (idx >= 0) {
                    choose(idx);
                }
            }
        }

        // ---------- animation ----------
        private static float ease(float dt, float speed) {
            return 1f - (float) Math.exp(-dt * speed);
        }

        private void step(float dt) {
            computeGeometry();
            int n = movies.size();
            if (n == 0) {
                return;
            }
            if (slotX.length != n) {
                slotX = new float[n];
                slotW = new float[n];
                hot = new float[n];
                weights = new float[n];
            }
            if (autoIdx >= n) {
                autoIdx = 0;
            }

            float total = Math.max(viewW, n * MIN_W);
            float maxPan = Math.max(0f, total - viewW);

            if (playing && !mouseIn) {
                autoTimer += dt;
                if (autoTimer > 3.6f) {
                    autoTimer = 0f;
                    autoIdx = (autoIdx + 1) % n;
                }
            }
            int rest = playing ? autoIdx : (sel >= 0 ? sel : autoIdx);

            float frac = mouseIn
                    ? Math.max(0f, Math.min(1f, (mouseX - viewX) / (float) viewW))
                    : (n > 1 ? rest / (float) (n - 1) : 0f);
            pan += (frac * maxPan - pan) * ease(dt, 2.5f);

            // The lens follows the cursor continuously (not poster-to-poster), and glides with a
            // speed limit so the strip moves in step with the mouse instead of snapping.
            float focusT;
            if (mouseIn) {
                focusT = Math.max(0.5f, Math.min(n - 0.5f, (mouseX - viewX) / (float) viewW * n));
            } else {
                focusT = rest + 0.5f;
            }
            if (focus < -0.5f) {
                focus = focusT;
            } else {
                float move = (focusT - focus) * ease(dt, 3.5f);
                float maxMove = MAX_LENS_SPEED * dt;
                focus += Math.max(-maxMove, Math.min(maxMove, move));
            }

            float sigma = 0.62f;
            float sum = 0f;
            for (int i = 0; i < n; i++) {
                float d = (i + 0.5f - focus) / sigma;
                weights[i] = 1f + EXPAND * (float) Math.exp(-d * d);
                sum += weights[i];
            }
            float cum = 0f;
            for (int i = 0; i < n; i++) {
                slotW[i] = total * weights[i] / sum;
                slotX[i] = viewX + cum - pan;
                cum += slotW[i];
            }

            hoverIdx = mouseIn ? slotAt(mouseX) : -1;
            int fi = hoverIdx >= 0 ? hoverIdx : (mouseIn ? -1 : rest);
            for (int i = 0; i < n; i++) {
                hot[i] += ((i == fi ? 1f : 0f) - hot[i]) * ease(dt, 5f);
            }
            disc += dt * (playing ? 1.4f : 0.15f);
        }

        // ---------- painting ----------
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            CustomerHomeFrame.aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(BG);
            g.fillRect(0, 0, w, h);
            computeGeometry();
            int n = movies.size();
            if (n == 0) {
                g.setFont(CustomerHomeFrame.Fonts.get(false, 16f));
                g.setColor(DIM);
                String msg = "No movies yet — fill in the form above and press Add.";
                g.drawString(msg, (w - g.getFontMetrics().stringWidth(msg)) / 2, h / 2);
                g.dispose();
                return;
            }
            if (slotX.length != n || (slotX[0] == 0f && slotW[0] == 0f)) {
                step(0f);
            }
            drawNames(g, n);
            drawPosters(g, n);
            drawOverlay(g);
            g.dispose();
        }

        private void drawNames(Graphics2D g, int n) {
            Shape oldClip = g.getClip();
            for (int i = 0; i < n; i++) {
                float x = slotX[i];
                float sw = slotW[i];
                float l = Math.max(x, viewX);
                float r = Math.min(x + sw, viewX + viewW);
                if (r - l < 40) {
                    continue;
                }
                float hv = hot[i];
                boolean isSel = i == sel;
                float emph = Math.max(hv, isSel ? 1f : 0f);

                g.setClip(oldClip);
                g.clipRect(Math.round(l), 0, Math.round(r - l) - 6, NAMES_H);
                float tx = Math.max(x, viewX) + 12;
                g.setFont(CustomerHomeFrame.Fonts.get(emph > 0.5f, 12.5f));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(CustomerHomeFrame.mix(new Color(120, 124, 134), TEXT, emph));
                String name = (i + 1) + ". " + CustomerHomeFrame.nz(movies.get(i).getTitle());
                g.drawString(CustomerHomeFrame.fit(name, fm, Math.round(r - tx) - 14), tx, 40);

                Path2D tri = new Path2D.Float();
                tri.moveTo(tx, 52);
                tri.lineTo(tx, 61);
                tri.lineTo(tx + 7.5f, 56.5f);
                tri.closePath();
                g.setColor(CustomerHomeFrame.mix(new Color(70, 72, 80), TEXT, emph));
                g.fill(tri);
            }
            g.setClip(oldClip);

            // pager dots: they follow the poster under the cursor (or the selected / playing one)
            int dots = Math.min(n, 12);
            float dx = viewX + viewW / 2f - (dots - 1) * 7f;
            for (int i = 0; i < dots; i++) {
                float lit = 0f;
                int from = i * n / dots;
                int to = Math.max(from + 1, (i + 1) * n / dots);
                for (int k = from; k < to && k < n; k++) {
                    lit = Math.max(lit, hot[k]);
                }
                float d = 4.4f + 2.6f * lit;
                g.setColor(CustomerHomeFrame.mix(new Color(80, 83, 92), TEXT, lit));
                g.fill(new Ellipse2D.Float(dx + i * 14f - d / 2f, NAMES_H - 8f - d / 2f, d, d));
            }
        }

        private void drawPosters(Graphics2D g, int n) {
            Shape oldClip = g.getClip();
            RoundRectangle2D shape = new RoundRectangle2D.Float(viewX, stripY, viewW, stripH, 20, 20);
            g.clip(shape);

            for (int i = 0; i < n; i++) {
                float x = slotX[i];
                float sw = slotW[i];
                if (x + sw < viewX || x > viewX + viewW) {
                    continue;
                }
                float hv = hot[i];
                Movie m = movies.get(i);
                float px = x + 1.5f;
                float pw = sw - 3f;
                Shape before = g.getClip();
                g.clipRect(Math.round(px), stripY, Math.round(pw), stripH);

                BufferedImage img = posters.get(m.getMovieId());
                if (img != null) {
                    drawCover(g, img, px, stripY, pw, stripH);
                } else {
                    drawFallback(g, m, px, stripY, pw, stripH);
                }
                float lit = Math.max(hv, i == sel ? 1f : 0f);
                int shade = Math.round((1f - lit) * 70);
                if (shade > 0) {
                    g.setColor(new Color(5, 5, 7, shade));
                    g.fill(new Rectangle2D.Float(px, stripY, pw, stripH));
                }
                if (i == sel) {
                    g.setColor(new Color(255, 255, 255, 150));
                    g.setStroke(new BasicStroke(2f));
                    g.draw(new Rectangle2D.Float(px + 1, stripY + 1, pw - 2, stripH - 2));
                }
                g.setClip(before);
            }
            g.setClip(oldClip);
            g.setColor(new Color(255, 255, 255, 22));
            g.setStroke(new BasicStroke(1f));
            g.draw(shape);
        }

        private void drawCover(Graphics2D g, BufferedImage img, float x, float y, float w, float h) {
            float pw = img.getWidth();
            float ph = img.getHeight();
            float scale = Math.max(w / pw, h / ph);
            float sw = w / scale;
            float sh = h / scale;
            float sx = (pw - sw) / 2f;
            float sy = (ph - sh) / 2f;
            g.drawImage(img,
                    Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h),
                    Math.round(sx), Math.round(sy), Math.round(sx + sw), Math.round(sy + sh), null);
        }

        private void drawFallback(Graphics2D g, Movie m, float x, float y, float w, float h) {
            String title = CustomerHomeFrame.nz(m.getTitle());
            float hue = (Math.abs(title.hashCode()) % 360) / 360f;
            g.setPaint(new GradientPaint(x, y, Color.getHSBColor(hue, 0.55f, 0.46f),
                    x, y + h, Color.getHSBColor((hue + 0.1f) % 1f, 0.6f, 0.10f)));
            g.fill(new Rectangle2D.Float(x, y, w, h));
            String ini = title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase();
            g.setFont(CustomerHomeFrame.Fonts.get(true, Math.min(w * 0.9f, h * 0.4f)));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(new Color(255, 255, 255, 38));
            g.drawString(ini, x + (w - fm.stringWidth(ini)) / 2f, y + h * 0.5f + fm.getAscent() * 0.3f);
        }

        /** "NOW MANAGING", the transport controls and the Selected label, over the bottom of the posters. */
        private void drawOverlay(Graphics2D g) {
            float bottom = stripY + stripH;
            Shape oldClip = g.getClip();
            g.clip(new RoundRectangle2D.Float(viewX, stripY, viewW, stripH, 20, 20));
            float gh = 190f;
            g.setPaint(new GradientPaint(0, bottom - gh, new Color(0, 0, 0, 0), 0, bottom, new Color(0, 0, 0, 215)));
            g.fill(new Rectangle2D.Float(viewX, bottom - gh, viewW, gh));
            g.setClip(oldClip);

            g.setFont(CustomerHomeFrame.Fonts.get(false, 44f));
            g.setColor(new Color(0, 0, 0, 110));
            CustomerHomeFrame.drawTracked(g, "NOW MANAGING", viewX + 30 + 1.5f, bottom - 30 + 1.5f, 2f);
            g.setColor(new Color(245, 246, 248));
            CustomerHomeFrame.drawTracked(g, "NOW MANAGING", viewX + 30, bottom - 30, 2f);

            float cy = ctrlCy();
            drawBtn(g, prevCx(), cy, 15, 0);
            drawBtn(g, playCx(), cy, 24, 1);
            drawBtn(g, nextCx(), cy, 15, 2);

            float dcx = viewX + viewW - 40;
            float dcy = bottom - 44;
            drawDisc(g, dcx, dcy, 22);
            String label = "Selected: " + (sel >= 0 && sel < movies.size() ? CustomerHomeFrame.nz(movies.get(sel).getTitle()) : "—");
            g.setFont(CustomerHomeFrame.Fonts.get(false, 13f));
            FontMetrics fm = g.getFontMetrics();
            String t = CustomerHomeFrame.fit(label, fm, 260);
            g.setColor(TEXT);
            g.drawString(t, dcx - 36 - fm.stringWidth(t), dcy + 5);
        }

        private void drawBtn(Graphics2D g, float cx, float cy, float r, int kind) {
            boolean over = controlAt(mouseX, mouseY) == kind;
            if (kind == 1) {
                g.setColor(new Color(20, 20, 24, 220));
                g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            }
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(over ? new Color(200, 204, 214) : new Color(110, 114, 124));
            g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            g.setColor(TEXT);
            if (kind == 1) {
                if (playing) {
                    g.fill(new RoundRectangle2D.Float(cx - 6, cy - 7.5f, 4, 15, 2, 2));
                    g.fill(new RoundRectangle2D.Float(cx + 2, cy - 7.5f, 4, 15, 2, 2));
                } else {
                    Path2D tri = new Path2D.Float();
                    tri.moveTo(cx - 5, cy - 8);
                    tri.lineTo(cx - 5, cy + 8);
                    tri.lineTo(cx + 8, cy);
                    tri.closePath();
                    g.fill(tri);
                }
            } else {
                float d = kind == 0 ? -1 : 1;
                Path2D tri = new Path2D.Float();
                tri.moveTo(cx - d * 1f, cy - 5);
                tri.lineTo(cx - d * 1f, cy + 5);
                tri.lineTo(cx + d * 5f, cy);
                tri.closePath();
                g.fill(tri);
                g.fill(new Rectangle2D.Float(cx - d * 5.5f - (d > 0 ? 0 : 1.6f), cy - 5, 1.6f, 10));
            }
        }

        private void drawDisc(Graphics2D g, float cx, float cy, float r) {
            AffineTransform old = g.getTransform();
            g.translate(cx, cy);
            g.rotate(disc);
            Color[] cols = {
                    new Color(110, 210, 255), new Color(190, 140, 255), new Color(255, 130, 180),
                    new Color(255, 190, 110), new Color(120, 230, 170), new Color(110, 210, 255)
            };
            for (int i = 0; i < 6; i++) {
                g.setColor(cols[i]);
                g.fill(new Arc2D.Float(-r, -r, r * 2, r * 2, i * 60, 60, Arc2D.PIE));
            }
            g.setColor(new Color(10, 10, 12, 215));
            g.fill(new Ellipse2D.Float(-r * 0.78f, -r * 0.78f, r * 1.56f, r * 1.56f));
            g.setColor(new Color(255, 255, 255, 40));
            g.setStroke(new BasicStroke(1f));
            g.draw(new Ellipse2D.Float(-r * 0.55f, -r * 0.55f, r * 1.1f, r * 1.1f));
            g.setColor(BG);
            g.fill(new Ellipse2D.Float(-r * 0.2f, -r * 0.2f, r * 0.4f, r * 0.4f));
            g.setTransform(old);
        }
    }
}