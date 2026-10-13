package graphics.rasterizer;

import model.Line;
import model.Polygon;

import java.util.List;

/**
 * Vykreslí obrys polygonu pomocí úseček.
 */
public class PolygonOutlineRasterizer implements PolygonRasterizer {

    private final LineRasterizer lineRasterizer;

    public PolygonOutlineRasterizer(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }

    @Override
    public void rasterize(Polygon polygon) {
        List<Line> lines = polygon.getLines();

        // vykreslíme všechny dosud vytvořené úsečky
        for (Line line : lines) {
            lineRasterizer.rasterize(line);
        }

        // uzavírací úsečku (poslední vrchol -> první vrchol) kreslíme,
        // jakmile má polygon alespoň 3 vrcholy (= 2 úsečky)
        if (lines.size() > 1) {
            Line last = lines.get(lines.size() - 1);
            Line first = lines.get(0);
            lineRasterizer.rasterize(new Line(last.getPoint2(), first.getPoint1(), last.getColor()));
        }
    }
}
