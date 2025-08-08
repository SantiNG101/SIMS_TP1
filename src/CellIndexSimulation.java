import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CellIndexSimulation {

    private final int N;
    private final double L; 
    private final int M; 
    private final double rc; 
    private final boolean periodic;
    private final List<Particle> particles;
    private final Map<Integer, List<Particle>> grid;
    private final double cellSize;
    private final long TIMESTEPS = 0;

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
        computeCellNeighbors();
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
        int cellX = Math.min((int) (p.getX() / cellSize), M - 1);
        int cellY = Math.min((int) (p.getY() / cellSize), M - 1);
        int cellIndex = cellX + cellY * M;
        grid.get(cellIndex).add(p);
    }
}


    private Map<Integer, List<Integer>> cellNeighbors;

private void computeCellNeighbors() {
    cellNeighbors = new HashMap<>();

    for (int cellY = 0; cellY < M; cellY++) {
        for (int cellX = 0; cellX < M; cellX++) {
            int cellIndex = cellX + cellY * M;
            List<Integer> neighbors = new ArrayList<>();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {

                    if (dx == 0 && dy == 0) continue;

                    int neighborCellX = cellX + dx;
                    int neighborCellY = cellY + dy;

                    if (periodic) {
                        neighborCellX = (neighborCellX + M) % M;
                        neighborCellY = (neighborCellY + M) % M;
                    }

                    if (!periodic && (neighborCellX < 0 || neighborCellX >= M || neighborCellY < 0 || neighborCellY >= M)) {
                        continue;
                    }

                    int neighborCellIndex = neighborCellX + neighborCellY * M;
                    neighbors.add(neighborCellIndex);
                }
            }

            cellNeighbors.put(cellIndex, neighbors);
        }
    }
}



    public Map<Integer, List<Integer>> findNeighborsCIM() {
        Map<Integer, List<Integer>> allNeighbors = new HashMap<>();
        for (int i = 0; i < N; i++) {
            allNeighbors.put(i, new ArrayList<>());
        }
        for (Particle p1 : particles) {
            List<Integer> neighbors = allNeighbors.get(p1.getId());
            int cellX = (int) (p1.getX() / cellSize);
            int cellY = (int) (p1.getY() / cellSize);
            int cellIndex = cellX + cellY * M;

            for (Particle p2 : grid.get(cellIndex)) {
                if (p1.getId() >= p2.getId()) continue;
                double dist = calculateDistance(p1, p2);
                if (dist - p1.getRadius() - p2.getRadius() < rc) {
                    allNeighbors.get(p1.getId()).add(p2.getId());
                    allNeighbors.get(p2.getId()).add(p1.getId());
                }
            }

            for (int neighborIndex : cellNeighbors.get(cellIndex)) {
                for (Particle p2 : grid.get(neighborIndex)) {
                    if (p1.getId() >= p2.getId()) continue;
                    double dist = calculateDistance(p1, p2);

                    if (dist - p1.getRadius() - p2.getRadius() < rc ) {
                        neighbors.add(p2.getId());
                        allNeighbors.get(p2.getId()).add(p1.getId());
                    }
                }
            }
        }

        Map<Integer, List<Integer>> result = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : allNeighbors.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        return result;
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

    public void writeOutputFiles(Map<Integer, List<Integer>> allNeighbors, boolean bruteForceMethod ) throws IOException {
        String fileName = bruteForceMethod? "bruteForce" : "CIM";

        try (FileWriter writer = new FileWriter("particles" + fileName + ".txt")) {
            writer.write(String.format("%d %f %d\n", N, L, M));
            for (Particle p : particles) {
                writer.write(p.toString() + "\n");
            }
        }

        try (FileWriter writer = new FileWriter("neighbors" + fileName + ".txt")) {
            for (Map.Entry<Integer, List<Integer>> entry : allNeighbors.entrySet()) {
                writer.write(entry.getKey().toString());
                for (Integer neighborId : entry.getValue()) {
                    writer.write(" " + neighborId);
                }
                writer.write("\n");
            }
        }

        try (FileWriter writer = new FileWriter("static" + fileName + ".txt")) {
            writer.write(String.format("%d %f \n", N, L));
            for (Particle p : particles) {
                writer.write(p.getStaticInfo() + "\n");
            }
        }

        try (FileWriter writer = new FileWriter("dynamic" + fileName + ".txt")) {
            for (long t = 0; t <= TIMESTEPS; t++) {
                writer.write("t="+t+"\n");
                for (Particle p : particles) {
                    writer.write(p.getDynamicInfo() + "\n");
                }
            }
        }

    }

    public static void main(String[] args) {
        int N = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        int M = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        double L = 20.0;
        double rc = 1.0;
        double r = 0.25;
        boolean periodic = false;
        System.out.println("Running Simulation with:");
        System.out.printf("N=%d, L=%.1f, M=%d, rc=%.1f, r=%.2f, periodic=%b\n", N, L, M, rc, r, periodic);
        System.out.println("----------------------------------------");

        CellIndexSimulation sim = new CellIndexSimulation(N, L, M, rc, r, periodic);

        long startTimeBF = System.nanoTime();
        Map<Integer, List<Integer>> neighborsBruteForceMap = sim.findNeighborsBruteForce();
        long endTimeBF = System.nanoTime();
        long durationBF = TimeUnit.NANOSECONDS.toMillis(endTimeBF - startTimeBF);
        System.out.println("Brute-Force Execution Time: " + durationBF + " ms");

        long startTimeCIM = System.nanoTime();
        Map<Integer, List<Integer>> neighborsCIMMap = sim.findNeighborsCIM();
        long endTimeCIM = System.nanoTime();
        long durationCIM = TimeUnit.NANOSECONDS.toMillis(endTimeCIM - startTimeCIM);
        System.out.println("Cell Index Method Execution Time: " + durationCIM + " ms");

        try {
            sim.writeOutputFiles(neighborsBruteForceMap,true);
            sim.writeOutputFiles(neighborsCIMMap,false);
            System.out.println("\nOutput files 'particles.txt' and 'neighbors.txt' generated for each method.");
            System.out.println("Run 'python visualize.py' to see the CIM's results.");
            System.out.println("Or run 'python visualizeComparison.py' to compare both method's results.");
        } catch (IOException e) {
            System.err.println("Error writing output files: " + e.getMessage());
        }
        boolean iguales = true;
        for (int i = 0; i < N; i++) {
            List<Integer> bruteNeighbors = neighborsBruteForceMap.get(i);
            List<Integer> cimNeighbors = neighborsCIMMap.get(i);
            if (!bruteNeighbors.containsAll(cimNeighbors) || !cimNeighbors.containsAll(bruteNeighbors)) {
                System.out.println("Diferencias en la partícula " + i);
                System.out.println("BruteForce: " + bruteNeighbors);
                System.out.println("CIM:        " + cimNeighbors);
                iguales = false;
            }
        }
        if (iguales) {
            System.out.println("✅ Ambos métodos dan los mismos vecinos.");
        } else {
            System.out.println("❌ Hay diferencias entre los métodos.");
        }
    }
}