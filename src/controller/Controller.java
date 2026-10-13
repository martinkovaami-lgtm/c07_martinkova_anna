package controller;

import graphics.rasterizer.LineRasterizer;
import graphics.rasterizer.PolygonOutlineRasterizer;
import graphics.rasterizer.PolygonRasterizer;
import graphics.rasterizer.TrivialLineRasterizer;
import model.Line;
import model.Point;
import model.Polygon;
import view.Canvas;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Zpracovává vstup od uživatele a řídí vykreslování na {@link Canvas}.
 * <ul>
 *   <li>LEVÉ tlačítko: stisknout, táhnout a pustit = přímá úsečka.</li>
 *   <li>PRAVÉ tlačítko: každé kliknutí přidá vrchol polygonu (od 3 bodů je polygon hotový a uzavřený).</li>
 *   <li>PROSTŘEDNÍ tlačítko nebo klávesa ENTER: zamkne polygon (už nejdou přidávat body).</li>
 *   <li>Klávesa C: smaže všechno.</li>
 * </ul>
 *
 * @author PGRF FIM UHK
 * @version 2026
 */
public class Controller {

    // barva hotových úseček a barva náhledu (červená)
    private static final int LINE_COLOR = Color.WHITE.getRGB();
    private static final int PREVIEW_COLOR = Color.RED.getRGB();

    private final Canvas canvas;
    private final LineRasterizer rasterizer;
    private final PolygonRasterizer polygonRasterizer;

    // polygon vytvářený pravým tlačítkem
    private final Polygon polygon = new Polygon();

    // samostatné úsečky vytvořené levým tlačítkem (tažením)
    private final List<Line> straightLines = new ArrayList<>();

    // poslední vrchol polygonu (null = zatím žádný). Potřebujeme ho, protože
    // po prvním kliknutí ještě neexistuje žádná úsečka, ze které bychom vzali koncový bod.
    private Point lastVertex;

    // náhled úsečky, kterou právě táhneme levým tlačítkem (null = netáhneme)
    private Line dragLine;

    public Controller(Canvas canvas) {
        this.canvas = canvas;
        this.rasterizer = new TrivialLineRasterizer(canvas.getRaster());
        this.polygonRasterizer = new PolygonOutlineRasterizer(rasterizer);
    }

    /* ---------------------------------------------- Hlavní funkce ---------------------------------------------- */

    public void init() {
        canvas.clear();

        MouseAdapter mouse = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                canvas.requestFocusInWindow(); // aby fungovala klávesnice

                if (SwingUtilities.isLeftMouseButton(e)) {
                    // LEVÉ tlačítko: začátek tažené úsečky (zatím nulové délky)
                    Point p = getPoint(e);
                    dragLine = new Line(p, p, PREVIEW_COLOR);
                    render();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    // PRAVÉ tlačítko: přidáme vrchol polygonu
                    startLine(e);
                } else if (SwingUtilities.isMiddleMouseButton(e)) {
                    // PROSTŘEDNÍ tlačítko: dokončíme polygon
                    finishPolygon();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // při tažení levým tlačítkem aktualizujeme koncový bod náhledu
                if (dragLine != null) {
                    dragLine = new Line(dragLine.getPoint1(), getPoint(e), PREVIEW_COLOR);
                    render();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // puštění levého tlačítka: tažená úsečka se stane trvalou
                if (SwingUtilities.isLeftMouseButton(e) && dragLine != null) {
                    Point start = dragLine.getPoint1();
                    Point end = getPoint(e);
                    dragLine = null; // náhled už nepotřebujeme

                    // prosté kliknutí bez tažení (start == konec) nic nevytvoří
                    if (start.getX() != end.getX() || start.getY() != end.getY()) {
                        straightLines.add(new Line(start, end, LINE_COLOR));
                    }
                    render();
                }
            }
        };
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);

        // Obsluha klávesnice
        canvas.setFocusable(true);
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_C) {
                    // C: smazat vše a umožnit kreslit od začátku
                    polygon.clear();       // odstraní úsečky a zruší příznak "dokončený"
                    straightLines.clear();
                    lastVertex = null;
                    dragLine = null;
                    render();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // ENTER: dokončit polygon
                    finishPolygon();
                }
            }
        });

        render();
        canvas.requestFocusInWindow();
    }

    /** Překreslí celou scénu od nuly. */
    private void render() {
        canvas.clear();

        // samostatné úsečky z levého tlačítka
        for (Line line : straightLines) {
            rasterizer.rasterize(line);
        }

        // polygon (od 3 vrcholů se hned kreslí i uzavírací úsečka - viz PolygonOutlineRasterizer)
        polygonRasterizer.rasterize(polygon);

        // po prvním kliknutí ještě není žádná úsečka - nakreslíme aspoň bod
        if (lastVertex != null && polygon.getLines().isEmpty()) {
            rasterizer.rasterize(new Line(lastVertex, lastVertex, LINE_COLOR));
        }

        // náhled úsečky, kterou právě táhneme levým tlačítkem
        if (dragLine != null) {
            rasterizer.rasterize(dragLine);
        }

        canvas.repaint();
    }

    /**
     * Zahájí/přidá novou úsečku polygonu při kliknutí pravým tlačítkem.
     */
    private void startLine(MouseEvent e) {
        // 1. dokončenému polygonu už nic nepřidáváme
        if (polygon.isFinished()) {
            return;
        }

        // 2. aktuální pozice myši
        Point actualPosition = getPoint(e);

        if (lastVertex == null) {
            // 3a. polygon je prázdný: toto kliknutí je první vrchol
            lastVertex = actualPosition;
        } else {
            // 3b. polygon už má vrchol: přidáme úsečku od posledního vrcholu k myši
            //     (poslední vrchol = koncový bod poslední úsečky)
            polygon.addLine(new Line(lastVertex, actualPosition, LINE_COLOR));
            lastVertex = actualPosition;
        }

        // 4. překreslit scénu (polygon se uzavře hned, jakmile má 3 vrcholy)
        render();
    }

    /**
     * Zamkne polygon: označí ho jako dokončený, takže už nejde přidávat další vrcholy.
     */
    private void finishPolygon() {
        // 1. polygon musí mít alespoň 3 vrcholy = alespoň 2 úsečky
        if (polygon.getLines().size() < 2 || polygon.isFinished()) {
            return;
        }

        // 2. označíme polygon jako dokončený
        polygon.setFinished(true);

        lastVertex = null;
        render();
    }

    /** Převede událost myši na bod (x, y) v rastru. */
    private Point getPoint(MouseEvent e) {
        return new Point(e.getX(), e.getY());
    }
}
