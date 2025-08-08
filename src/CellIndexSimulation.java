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
            int cellX = (int) (p.getX() / cellSize);
            int cellY = (int) (p.getY() / cellSize);
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

                // (0,0) (0,1) (1,-1) (1,0) (1,1) 
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if ( dx==0 && dy==-1 ) continue;
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

            for (int neighborIndex : cellNeighbors.get(cellIndex)) {
                for (Particle p2 : grid.get(neighborIndex)) {
                    if (neighborIndex==cellIndex && p1.getId() >= p2.getId()) continue;

                    double dist = calculateDistance(p1, p2);

                    if (dist - p1.getRadius() - p2.getRadius() < rc ) {
                        neighbors.add(p2.getId());
                        allNeighbors.get(p2.getId()).add(p1.getId());
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

    public void writeOutputFiles(Map<Integer, List<Integer>> allNeighbors, boolean bruteForceMethod, int M, double L, double rc ) throws IOException {
        String method = bruteForceMethod? "bruteForce" : "CIM";
        String fileName = String.format("_M%d_L%.1f_rc%.1f_%s", M, L, rc, method);

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

        int[] N_values = {100};
        int[] M_values = {5, 10, 15};
        double[] L_values = {20.0};
        double[] rc_values = {1.0};

        double r = 0.25;
        boolean periodic = false;

        try (FileWriter writer = new FileWriter("resultados.txt")) {
            writer.write("M,L,rc,N,metodo,tiempo_ms\n");
        

            for (int N : N_values) {
                for (int M : M_values) {
                    for (double L : L_values) {
                        for (double rc : rc_values) {

                            System.out.println("----------------------------------------");
                            System.out.printf("Ejecutando N=%d, L=%.1f, M=%d, rc=%.1f, r=%.2f, periodic=%b\n", N, L, M, rc, r, periodic);

                            if(L / M > rc && M < L)  {
                                CellIndexSimulation sim = new CellIndexSimulation(N, L, M, rc, r, periodic);

                                long startTimeBF = System.nanoTime();
                                Map<Integer, List<Integer>> neighborsBruteForceMap = sim.findNeighborsBruteForce();
                                long endTimeBF = System.nanoTime();
                                double durationBF = (endTimeBF - startTimeBF) / 1_000_000.0; // milisegundos con decimales
                                writer.write(String.format("%d,%.1f,%.2f,%d,%s,%.3f\n", M, L, rc, N, "FuerzaBruta", durationBF));

                                long startTimeCIM = System.nanoTime();
                                Map<Integer, List<Integer>> neighborsCIMMap = sim.findNeighborsCIM();
                                long endTimeCIM = System.nanoTime();
                                double durationCIM = (endTimeCIM - startTimeCIM) / 1_000_000.0; // milisegundos con decimales
                                writer.write(String.format("%d,%.1f,%.2f,%d,%s,%.3f\n", M, L, rc, N, "CellIndex", durationCIM));

                                try {
                                    sim.writeOutputFiles(neighborsBruteForceMap,true, M, L, rc);
                                    sim.writeOutputFiles(neighborsCIMMap,false, M, L, rc);
                                } catch (IOException e) {
                                    System.err.println("Error writing output files: " + e.getMessage());
                                }
                                
                                boolean iguales = true;
                                for (int i = 0; i < N; i++) {
                                    List<Integer> bruteNeighbors = neighborsBruteForceMap.get(i);
                                    List<Integer> cimNeighbors = neighborsCIMMap.get(i);
                                    if (!bruteNeighbors.containsAll(cimNeighbors) || !cimNeighbors.containsAll(bruteNeighbors)) {
                                        iguales = false;
                                    }
                                }
                                if (iguales) {
                                    System.out.println("✅ Ambos métodos dan los mismos vecinos.");
                                } else {
                                    System.out.println("❌ Hay diferencias entre los métodos.");
                                }
                            } else System.out.printf("❌ Saltando (N=%d, L=%.1f, M=%d, rc=%.1f) porque L/M <= rc\n", N, L, M, rc);
                        }
                    }
                }
            }
        } catch (IOException e) {
        System.err.println("Error escribiendo archivo: " + e.getMessage());
        }
    }
}