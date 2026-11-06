package graphics.filler;

import graphics.Raster;
import model.Line;
import model.Point;
import model.Polygon;

import java.util.ArrayList;
import java.util.List;

public class ScanlineFiller implements PolygonFiller{
    private final Raster raster;

    public ScanlineFiller(Raster raster){
        this.raster = raster;
    }


    @Override
    public void fill(Polygon polygon, int fillColor) {

        List<Line> edges = new ArrayList<Line>(polygon.getLines());
        if (edges.size() <2){
            return;
        }
        // a uzavreni polygonu spojenim posledniho a prvniho  bodu
        Line last = edges.getLast();
        Line first = edges.getFirst();
        edges.add(new Line(last.getPoint2(), first.getPoint1(), last.getColor()));

        // b zajisteni spravne orientace hran (A -> B) a urceni rozsahu

        int maxY = 0;
        int minY = raster.getHeight();
        for (int i = 0; i<edges.size(); i++){
            Line edge = edges.get(i);
            Point p1 = edge.getPoint1();
            Point p2 = edge.getPoint2();


            if(p1.getY()>p2.getY()){
                Point temp = p1;
                p1 = p2;
                p2 = temp;
                edges.set(i, new Line(p1, p2, fillColor));
            }

            minY = Math.min(minY, p1.getY());
            maxY = Math.max(maxY, p2.getY());

        }

        // c vypocet pruseciku hran se stredem kazdeho radku
        List<Double> intersections = new ArrayList<>();
        for(int y =Math.max(0, minY); y<Math.min(raster.getHeight(), maxY); y++){
            intersections.clear();
            double scanY = y - 0.5;

            //Vypocet pruseciku
            for(Line edge : edges){
                Point p1 = edge.getPoint1();
                Point p2 = edge.getPoint2();
                //Odstraneni rovinnych car + spodniho bodu
                if (y<p1.getY()||y>=p2.getY()){
                    continue;
                }

                //Dopocitame jednotlive souradnice x
                double x = p1.getX()+(scanY- p1.getY()) * (p2.getX() - p1.getX())/(p2.getY() - p1.getY());
                intersections.add(x);

            }
            //Serazeni pruseciku a vyplneni pixelu mezi nimi
            intersections.sort(null);
            for (int i = 0; i+1<intersections.size(); i=i+2){
                //Dopocitame (zaokoruhlime) xovou souradnici odkud mame jit
                int from = Math.max(0, (int) Math.ceil(intersections.get(i)-0.5));


                //Dopocitame (zaokoruhlime) xovou souradnici kam mame jit
                int to = Math.min(raster.getWidth(), (int) Math.ceil(intersections.get(i+1)-0.5));

                //Projdeme od from do to a obarvime pixely
                for (int x = from; x<to; x++){
                    raster.setPixel(x,y,fillColor);
                }


            }
        }

    }
}
