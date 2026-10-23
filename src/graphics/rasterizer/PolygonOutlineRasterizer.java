package graphics.rasterizer;

import model.Line;
import model.Polygon;

import java.util.ArrayList;
import java.util.List;


 //Vykreslí obrys polygonu pomocí úseček.

public class PolygonOutlineRasterizer implements PolygonRasterizer {

    private final LineRasterizer lineRasterizer;

    public PolygonOutlineRasterizer(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }

    @Override
    public void rasterize(Polygon polygon, Line previewLine) {
        List<Line> lines = new ArrayList<>(polygon.getLines());

        // pružná čára se chová jako poslední úsečka polygonu
        if (previewLine != null) {
            lines.add(previewLine);
        }

        // vykreslím všechny dosud vytvořené úsečky
        for (Line line : lines) {
            lineRasterizer.rasterize(line);
        }

        // uzavírací úsečka vede z konce poslední úsečky (u náhledu z kurzoru) zpět do prvního bodu,
        // takže při tažení jsou vidět dvě pružné čáry
        if (lines.size() > 1) {
            Line last = lines.get(lines.size() - 1);
            Line first = lines.get(0);
            lineRasterizer.rasterize(new Line(last.getPoint2(), first.getPoint1(), last.getColor()));
        }
    }
}
