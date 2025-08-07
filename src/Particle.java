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

    public String getStaticInfo() {
        return String.format("%.4f %.4f", radius, Math.PI * Math.pow(radius, 2));
    }

    public String getDynamicInfo() {
        return String.format("%.4f %.4f", x, y);
    }
}
