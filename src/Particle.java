public class Particle {
    private final int id;
    private final double x;
    private final double y;
    private final double radius;

    public Particle(int id, double x, double y, double radius) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }

    @Override
    public String toString() {
        return String.format("%d %.4f %.4f %.4f", id, x, y, radius);
    }
}
