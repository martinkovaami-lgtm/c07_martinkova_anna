package model;

import java.util.ArrayList;
import java.util.List;

public class Polygon {

    // úsečky polygonu
    private final List<Line> lines;

    // true = polygon je hotový (uzavřený)
    private boolean isFinished = false;

    public Polygon() {
        this(new ArrayList<>());
    }

    public Polygon(List<Line> lines) {

        this.lines = new ArrayList<>(lines);
    }
    public Point getLastPoint(){return lines.isEmpty() ? null : lines.getLast().getPoint2();}

    public List<Line> getLines() {
        return lines;
    }

    //Přidá úsečku, ale jen pokud polygon ještě není dokončený
    public void addLine(Line line) {
        if (!isFinished) {
            lines.add(line);
        }
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        this.isFinished = finished;
    }

    // Smaže všechny úsečky a obnoví stav, aby šlo kreslit nový polygon
    public void clear() {
        lines.clear();
        isFinished = false;
    }
}
