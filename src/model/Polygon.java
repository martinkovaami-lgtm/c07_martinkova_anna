package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Polygon složený z úseček. Má příznak "dokončený" - dokud není dokončený,
 * rasterizér nekreslí uzavírací úsečku mezi posledním a prvním vrcholem.
 */
public class Polygon {

    // úsečky polygonu (měnitelný seznam, aby šly přidávat další)
    private final List<Line> lines;

    // true = polygon je hotový (uzavřený), už nejde přidávat další úsečky
    private boolean isFinished = false;

    public Polygon() {
        this(new ArrayList<>());
    }

    public Polygon(List<Line> lines) {
        // uděláme si vlastní kopii seznamu, aby šlo bezpečně přidávat úsečky
        this.lines = new ArrayList<>(lines);
    }

    public List<Line> getLines() {
        return lines;
    }

    /** Přidá úsečku, ale jen pokud polygon ještě není dokončený. */
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

    /** Smaže všechny úsečky a obnoví stav, aby šlo kreslit nový polygon. */
    public void clear() {
        lines.clear();
        isFinished = false;
    }
}
