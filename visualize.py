import matplotlib.pyplot as plt
import matplotlib.patches as patches
import random

def visualize_simulation():

    particles = {}
    all_neighbors = {}

    try:
        with open('particlesCIM.txt', 'r') as f:
            header = f.readline().strip().split()
            N, L, M = int(header[0]), float(header[1]), int(header[2])
            for line in f:
                parts = line.strip().split()
                p_id, x, y, r = int(parts[0]), float(parts[1]), float(parts[2]), float(parts[3])
                particles[p_id] = {'x': x, 'y': y, 'r': r}
    except FileNotFoundError:
        print("Error: 'particlesCIM.txt' no encontrado. Ejecuta la simulación en Java primero.")
        return

    try:
        with open('neighborsCIM.txt', 'r') as f:
            for line in f:
                parts = [int(p) for p in line.strip().split()]
                p_id = parts[0]
                neighbors = set(parts[1:])
                all_neighbors[p_id] = neighbors
    except FileNotFoundError:
        print("Error: 'neighborsCIM.txt' no encontrado. Ejecuta la simulación en Java primero.")
        return

    # Elegir una partícula aleatoria
    special_particle = random.choice(list(particles.keys()))
    special_neighbors = all_neighbors.get(special_particle, set())

    print(special_particle)

    # --- Gráfico ---
    fig, ax = plt.subplots(figsize=(8, 8))
    ax.set_xlim(0, L)
    ax.set_ylim(0, L)
    ax.set_aspect('equal', adjustable='box')

    cell_size = L / M
    for i in range(M + 1):
        pos = i * cell_size
        ax.axhline(pos, color='lightgray', linestyle='--', linewidth=0.5)
        ax.axvline(pos, color='lightgray', linestyle='--', linewidth=0.5)

    # Dibujar partículas con colores especiales
    for p_id, data in particles.items():
        if p_id == special_particle:
            color = 'orange'
        elif p_id in special_neighbors:
            color = 'lightgreen'
        else:
            color = 'skyblue'
        circle = patches.Circle((data['x'], data['y']), radius=data['r'],
                                facecolor=color, edgecolor='black', linewidth=0.75, zorder=10)
        ax.add_patch(circle)

    # Dibujar líneas entre vecinos
    drawn_pairs = set()
    for p_id, neighbors in all_neighbors.items():
        p1_coords = (particles[p_id]['x'], particles[p_id]['y'])
        for neighbor_id in neighbors:
            pair = tuple(sorted((p_id, neighbor_id)))
            if pair not in drawn_pairs:
                p2_coords = (particles[neighbor_id]['x'], particles[neighbor_id]['y'])
                ax.plot([p1_coords[0], p2_coords[0]], [p1_coords[1], p2_coords[1]],
                        color='red', linestyle='-', linewidth=1.5, zorder=1)
                drawn_pairs.add(pair)

    ax.set_title(f'Visualización de vecinos (N={N}, M={M}x{M})')
    ax.set_xlabel('Posición X')
    ax.set_ylabel('Posición Y')
    plt.grid(False)
    plt.show()


if __name__ == '__main__':
    visualize_simulation()
