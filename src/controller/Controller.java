package controller;

import graphics.clipping.PolygonClipper;
import graphics.clipping.SutherlandHodgmanClipper;
import graphics.filler.RecursiveSeedFiller;
import graphics.filler.ScanlineFiller;
import graphics.filler.SeedFiller;
import graphics.filler.StackSeedFiller;
import graphics.rasterizer.LineRasterizer;
import graphics.rasterizer.PolygonOutlineRasterizer;
import graphics.rasterizer.PolygonRasterizer;
import graphics.rasterizer.TrivialLineRasterizer;
import model.Line;
import model.Point;
import model.Polygon;
import view.Canvas;
import view.gui.Toolbar;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class Controller {

    // tady je barva hotových úseček a barva náhledu
    private static final int LINE_COLOR = Color.WHITE.getRGB();
    private static final int PREVIEW_COLOR = Color.RED.getRGB();
    private static final int FILL_COLOR = new Color(0x58BFA9).getRGB();

    // barvy pro ořezávání: původní polygon, ořezaný polygon a ořezávací trojúhelník
    private static final int ORIGINAL_COLOR = new Color(0x9099A3).getRGB();
    private static final int CLIPPED_COLOR = new Color(0xA4F2DF).getRGB();
    private static final int CLIPPING_TRIANGLE_COLOR = new Color(0xE6B85C).getRGB();

    // vrcholy ořezávacího trojúhelníku (konvexní polygon)
    private static final List<Point> CLIPPING_TRIANGLE_VERTICES = List.of(
            new Point(250, 100),
            new Point(480, 470),
            new Point(40, 470)
    );

    private final Canvas canvas;
    private final Toolbar toolbar;
    private final LineRasterizer rasterizer;
    private final PolygonRasterizer polygonRasterizer;
    private final SeedFiller seedFiller;
    private final ScanlineFiller scanlineFiller;
    private final PolygonClipper clipper;
    private final Polygon clippingTriangle;


    // polygon, který je vytvářený pravým tlačítkem
    private final Polygon polygon = new Polygon();

    // úsečky vytvořené levým tlačítkem
    private final List<Line> straightLines = new ArrayList<>();

    // poslední vrchol polygonu
    private Point lastVertex;

    // náhled úsečky, kterou právě táhnu levým tlačítkem
    private Line dragLine;

    private Line previewPolygonLine;

    // bod, ze kterého se vyplňuje (seed fill), a režim výběru tohoto bodu
    private Point seedFillPoint;
    private boolean seedFillMode;


    public Controller(Canvas canvas, Toolbar toolbar) {
        this.canvas = canvas;
        this.toolbar = toolbar;
        this.rasterizer = new TrivialLineRasterizer(canvas.getRaster());
        this.polygonRasterizer = new PolygonOutlineRasterizer(rasterizer);
        this.seedFiller = new StackSeedFiller(canvas.getRaster());
        this.scanlineFiller = new ScanlineFiller(canvas.getRaster());
        this.clipper = new SutherlandHodgmanClipper();
        this.clippingTriangle = createPolygon(CLIPPING_TRIANGLE_VERTICES, CLIPPING_TRIANGLE_COLOR);


    }


    /* ---------------------------------------------- Hlavní funkce ---------------------------------------------- */

    public void init() {
        canvas.clear();

        // tlačítka v toolbaru
        toolbar.addClearListener(e -> {
            clearDrawing();
            canvas.requestFocusInWindow();
        });
        toolbar.addSeedFillListener(e -> {
            toggleSeedFillMode();
            canvas.requestFocusInWindow();
        });



        MouseAdapter mouse = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                canvas.requestFocusInWindow(); // aby fungovala klávesnice

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (seedFillMode) {
                        // v režimu seed fill levé tlačítko vybírá bod pro výplň
                        fillAt(e);
                    } else {
                        // LEVÉ tlačítko: začátek té úsečky (zatím má nulovou délku)
                        Point p = getPoint(e);
                        dragLine = new Line(p, p, PREVIEW_COLOR);
                        render();
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    // PRAVÉ tlačítko: přidám vrchol pro polygon
                    if (!polygon.isFinished()){
                        Point p = getPoint(e);
                        if (lastVertex == null){
                            lastVertex = p;
                        }

                        previewPolygonLine = new Line(lastVertex, p, PREVIEW_COLOR);
                        render();
                    }
                }
                else {
                    startLine(e);
                }

            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // při tažení levým tlačítkem se aktualizuje koncový bod náhledu
                if (SwingUtilities.isLeftMouseButton(e) && dragLine != null) {
                    // Aktualizace pružné čáry pro úsečku
                    dragLine = new Line(dragLine.getPoint1(), getPoint(e), PREVIEW_COLOR);
                    render();
                } else if (SwingUtilities.isRightMouseButton(e) && previewPolygonLine != null && !polygon.isFinished()) {
                    // Aktualizace pružné čáry pro polygon
                    previewPolygonLine = new Line(lastVertex, getPoint(e), PREVIEW_COLOR);
                    render();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point endPoint = getPoint(e);
                // puštění levého tlačítka: úsečka se stane trvalou
                if (SwingUtilities.isLeftMouseButton(e) && dragLine != null) {
                    Point start = dragLine.getPoint1();
                    Point end = dragLine.getPoint2();
                    dragLine = null; // náhled už nepotřebujeme

                    // Když jenom klikneš a netáhneš (start == konec) nic nevytvoří
                    if (start.getX() != end.getX() || start.getY() != end.getY()) {
                        straightLines.add(new Line(start, end, LINE_COLOR));
                    }
                    render();

                }
                else if (SwingUtilities.isRightMouseButton(e) && previewPolygonLine != null && !polygon.isFinished()) {
                    // Puštění pravého tlačítka: přidání hrany a vrcholu do polygonu
                    previewPolygonLine = null; // smazání náhledu

                    // Přidáme hranu z předchozího vrcholu do aktuálního místa puštění
                    polygon.addLine(new Line(lastVertex, endPoint, LINE_COLOR));

                    // Tento nový bod se stává výchozím vrcholem pro další tažení
                    lastVertex = endPoint;
                    render();
                }
            }
        };
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);

        // ovládání klávecnice
        canvas.setFocusable(true);
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_C) {
                    // C: smazat vše a umožnit kreslit od začátku
                    clearDrawing();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // ENTER: dokončit polygon
                    finishPolygon();
                }
            }
        });

        render();
        canvas.requestFocusInWindow();
    }

    //Překreslí celou scénu
    private void render() {
        canvas.clear();

        // ořezání polygonu (včetně pružné čáry) ořezávacím trojúhelníkem - Sutherland-Hodgman
        Polygon subject = withPreviewLine(polygon, previewPolygonLine);
        Polygon clipped = clipper.clip(subject, clippingTriangle);

        // výplň (scanline) se kreslí první, aby obrysy zůstaly nad ní
        scanlineFiller.fill(clipped, FILL_COLOR);

        // samostatné úsečky z levého tlačítka
        for (Line line : straightLines) {
            rasterizer.rasterize(line);
        }

        // polygon (od 3 vrcholů se hned kreslí i uzavírací úsečka)
        polygonRasterizer.rasterize(polygon, previewPolygonLine);

        // po prvním kliknutí ještě není žádná úsečka - nakreslí se bod
        if (lastVertex != null && polygon.getLines().isEmpty()) {
            rasterizer.rasterize(new Line(lastVertex, lastVertex, LINE_COLOR));
        }

        // náhled úsečky, kterou právě táhnu levým tlačítkem
        if (dragLine != null) {
            rasterizer.rasterize(dragLine);
        }

        // ořezaný polygon a ořezávací trojúhelník
        polygonRasterizer.rasterize(withColor(clipped, CLIPPED_COLOR), null);
        polygonRasterizer.rasterize(clippingTriangle, null);

        if (seedFillPoint != null) {
            seedFiller.fill(seedFillPoint.getX(), seedFillPoint.getY(), canvas.getBackgroundColor(), FILL_COLOR);
        }



        canvas.repaint();
    }




     //přidá novou úsečku polygonu při kliknutí pravým tlačítkem

    private void startLine(MouseEvent e) {

        Point startPoint = polygon.getLastPoint();
        if (startPoint == null) {
            startPoint = getPoint(e);
        }
        dragLine = new Line(startPoint, startPoint, PREVIEW_COLOR);
        render();
    }

    private void clearDrawing() {
        polygon.clear();
        straightLines.clear();
        lastVertex = null;
        dragLine = null;
        previewPolygonLine = null;
        seedFillPoint = null;
        seedFillMode = false;
        toolbar.setSeedFillMode(false);
        render();
    }

    // Zapne / vypne režim výběru bodu pro seed fill
    private void toggleSeedFillMode() {
        seedFillMode = !seedFillMode;
        toolbar.setSeedFillMode(seedFillMode);
    }

    // Klik v režimu seed fill: na prázdné místo vloží výplň, na vyplněnou oblast ji zruší
    private void fillAt(MouseEvent e) {
        Point seed = getPoint(e);
        Integer seedColor = canvas.getRaster().getPixel(seed.getX(), seed.getY());
        if (seedColor == null) {
            return;
        }

        if (seedFillPoint != null && seedColor == FILL_COLOR) {
            seedFillPoint = null;
            render();
        } else if (seedColor == canvas.getBackgroundColor()) {
            seedFillPoint = seed;
            render();
        }
    }


      //"Zamkne" polygon: označí ho jako dokončený, takže už nejde přidávat další vrcholy (body)

    private void finishPolygon() {
        // 1. polygon musí mít alespoň 3 vrcholy = alespoň 2 úsečky
        if (polygon.getLines().size() < 2 || polygon.isFinished()) {
            return;
        }

        // 2. označím polygon jako dokončený
        polygon.setFinished(true);

        lastVertex = null;
        previewPolygonLine = null;
        render();
    }

    private Polygon withColor(Polygon source, int color) {
        Polygon result = new Polygon();
        for (Line line : source.getLines()) {
            result.addLine(new Line(line.getPoint1(), line.getPoint2(), color));
        }
        return result;
    }

    // vrátí kopii polygonu rozšířenou o pružnou čáru (pokud existuje)
    private Polygon withPreviewLine(Polygon source, Line preview) {
        Polygon result = new Polygon();
        for (Line line : source.getLines()) {
            result.addLine(line);
        }
        if (preview != null) {
            result.addLine(preview);
        }
        return result;
    }

    // vytvoří polygon (otevřený řetězec úseček) ze seznamu vrcholů
    private Polygon createPolygon(List<? extends Point> vertices, int color) {
        Polygon result = new Polygon();
        for (int i = 0; i + 1 < vertices.size(); i++) {
            result.addLine(new Line(vertices.get(i), vertices.get(i + 1), color));
        }
        return result;
    }

    //Převede událost myši na bod (x, y) v rastru
    private Point getPoint(MouseEvent e) {
        return new Point(e.getX(), e.getY());
    }
}
