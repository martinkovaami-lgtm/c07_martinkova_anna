package graphics.rasterizer;

import graphics.Raster;
import model.Line;

/**
 * Jednoduchý rasterizér úsečky (DDA / rovnice přímky y = k*x + q).
 */
public class TrivialLineRasterizer implements LineRasterizer {

    private final Raster raster;

    public TrivialLineRasterizer(Raster raster) {
        this.raster = raster;
    }

    @Override
    public void rasterize(Line line) {
        rasterize(
                line.getPoint1().getX(),
                line.getPoint1().getY(),
                line.getPoint2().getX(),
                line.getPoint2().getY(),
                line.getColor()
        );
    }

    private void rasterize(int x1, int y1, int x2, int y2, int color) {
        // začátek a konec splývají -> vykreslíme jen jeden pixel
        if (x1 == x2 && y1 == y2) {
            raster.setPixel(x1, y1, color);
            return;
        }

        // svislá úsečka (x1 == x2) - nelze použít směrnici k (dělení nulou)
        if (x1 == x2) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                raster.setPixel(x1, y, color);
            }
            return;
        }

        // vždy kreslíme zleva doprava - pokud je x2 < x1, prohodíme body
        if (x2 < x1) {
            int t = x1; x1 = x2; x2 = t;
            t = y1; y1 = y2; y2 = t;
        }

        // směrnice k a posun q rovnice přímky y = k*x + q
        float k = (y2 - y1) / (float) (x2 - x1);
        float q = y1 - k * x1;

        if (Math.abs(y2 - y1) > Math.abs(x2 - x1)) {
            // strmá úsečka: jdeme po ose y a x dopočítáme (x = (y - q) / k)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                raster.setPixel(Math.round((y - q) / k), y, color);
            }
        } else {
            // plochá úsečka: jdeme po ose x a y dopočítáme (y = k*x + q)
            for (int x = x1; x <= x2; x++) {
                raster.setPixel(x, Math.round(k * x + q), color);
            }
        }
    }
}
