import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CellIndexSimulation {

    private final int N; // Number of particles
    private final double L; // Side length of the area
    private final int M; // Number of cells per row/column
    private final double rc; // Interaction radius
    private final boolean periodic;
    private final List<Particle> particles;
    private final Map<Integer, List<Particle>> grid;
    private final double cellSize;

    public CellIndexSimulation(int N, double L, int M, double rc, double particleRadius, boolean periodic) {
        this.N = N;
        this.L = L;
        this.M = M;
        this.rc = rc;
        this.periodic = periodic;
        this.particles = new ArrayList<>(N);
        this.grid = new HashMap<>();
        this.cellSize = L / M;

        generateParticles(particleRadius);
        initializeGrid();
    }

    private void generateParticles(double radius) {
        Random rand = new Random();
        for (int i = 0; i < N; i++) {
            particles.add(new Particle(
                    i,
                    rand.nextDouble() * L,
                    rand.nextDouble() * L,
                    radius
            ));
        }
    }

    private void initializeGrid() {
        for (int i = 0; i < M * M; i++) {
            grid.put(i, new ArrayList<>());
        }
        for (Particle p : particles) {
            int cellX = (int) (p.getX() / cellSize);
            int cellY = (int) (p.getY() / cellSize);
            int cellIndex = cellX + cellY * M;
            grid.get(cellIndex).add(p);
        }
    }

    public Map<Integer, List<Integer>> findNeighborsCIM() {
        Map<Integer, List<Integer>> allNeighbors = new HashMap<>();
        for (Particle p1 : particles) {
            List<Integer> neighbors = new ArrayList<>();
            int cellX = (int) (p1.getX() / cellSize);
            int cellY = (int) (p1.getY() / cellSize);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int neighborCellX = cellX + dx;
                    int neighborCellY = cellY + dy;

                    if (periodic) {
                        neighborCellX = (neighborCellX + M) % M;
                        neighborCellY = (neighborCellY + M) % M;
                    }

                    if (!periodic && (neighborCellX < 0 || neighborCellX >= M || neighborCellY < 0 || neighborCellY >= M)) {
                        continue;
                    }

                    int cellIndex = neighborCellX + neighborCellY * M;
                    for (Particle p2 : grid.get(cellIndex)) {
                        if (p1.getId() == p2.getId()) continue;

                        double dist = calculateDistance(p1, p2);

                        if (dist - p1.getRadius() - p2.getRadius() < rc) {
                            neighbors.add(p2.getId());
                        }
                    }
                }
            }
            allNeighbors.put(p1.getId(), neighbors);
        }
        return allNeighbors;
    }

    public Map<Integer, List<Integer>> findNeighborsBruteForce() {
        Map<Integer, List<Integer>> allNeighbors = new HashMap<>();
        for (int i = 0; i < N; i++) {
            allNeighbors.put(i, new ArrayList<>());
        }

        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                Particle p1 = particles.get(i);
                Particle p2 = particles.get(j);
                double dist = calculateDistance(p1, p2);

                if (dist - p1.getRadius() - p2.getRadius() < rc) {
                    allNeighbors.get(p1.getId()).add(p2.getId());
                    allNeighbors.get(p2.getId()).add(p1.getId());
                }
            }
        }
        return allNeighbors;
    }

    private double calculateDistance(Particle p1, Particle p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();

        if (periodic) {
            if (dx > L / 2) dx -= L;
            if (dx < -L / 2) dx += L;
            if (dy > L / 2) dy -= L;
            if (dy < -L / 2) dy += L;
        }

        return Math.sqrt(dx * dx + dy * dy);
    }

    public void writeOutputFiles(Map<Integer, List<Integer>> allNeighbors) throws IOException {
        try (FileWriter writer = new FileWriter("particles.txt")) {
            writer.write(String.format("%d %f %d\n", N, L, M));
            for (Particle p : particles) {
                writer.write(p.toString() + "\n");
            }
        }


        try (FileWriter writer = new FileWriter("neighbors.txt")) {
            for (Map.Entry<Integer, List<Integer>> entry : allNeighbors.entrySet()) {
                writer.write(entry.getKey().toString());
                for (Integer neighborId : entry.getValue()) {
                    writer.write(" " + neighborId);
                }
                writer.write("\n");
            }
        }
    }

    public static void main(String[] args) {
        int N = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        int M = args.length > 1 ? Integer.parseInt(args[1]) : 10;
        double L = 20.0;
        double rc = 1.0;
        double r = 0.25;
        boolean periodic = false;
        System.out.println("Running Simulation with:");
        System.out.printf("N=%d, L=%.1f, M=%d, rc=%.1f, r=%.2f, periodic=%b\n", N, L, M, rc, r, periodic);
        System.out.println("----------------------------------------");

        CellIndexSimulation sim = new CellIndexSimulation(N, L, M, rc, r, periodic);

        long startTimeBF = System.nanoTime();
        sim.findNeighborsBruteForce();
        long endTimeBF = System.nanoTime();
        long durationBF = TimeUnit.NANOSECONDS.toMillis(endTimeBF - startTimeBF);
        System.out.println("Brute-Force Execution Time: " + durationBF + " ms");

        long startTimeCIM = System.nanoTime();
        Map<Integer, List<Integer>> neighborsMap = sim.findNeighborsCIM();
        long endTimeCIM = System.nanoTime();
        long durationCIM = TimeUnit.NANOSECONDS.toMillis(endTimeCIM - startTimeCIM);
        System.out.println("Cell Index Method Execution Time: " + durationCIM + " ms");

        try {
            sim.writeOutputFiles(neighborsMap);
            System.out.println("\nOutput files 'particles.txt' and 'output_neighbors.txt' generated.");
            System.out.println("Run 'python visualize.py' to see the results.");
        } catch (IOException e) {
            System.err.println("Error writing output files: " + e.getMessage());
        }
    }
}