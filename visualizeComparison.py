import matplotlib.pyplot as plt
import matplotlib.patches as patches

def load_data(particles_file, neighbors_file):
    particles = {}
    neighbors = {}

    with open(particles_file, 'r') as f:
        header = f.readline().strip().split()
        N, L, M = int(header[0]), float(header[1]), int(header[2])
        for line in f:
            parts = line.strip().split()
            p_id, x, y, r = int(parts[0]), float(parts[1]), float(parts[2]), float(parts[3])
            particles[p_id] = {'x': x, 'y': y, 'r': r}

    with open(neighbors_file, 'r') as f:
        for line in f:
            parts = [int(p) for p in line.strip().split()]
            p_id = parts[0]
            neighbors[p_id] = set(parts[1:])

    return N, L, M, particles, neighbors


def visualize_comparison():
    try:
        N1, L1, M1, particles_cim, neighbors_cim = load_data("particlesCIM.txt", "neighborsCIM.txt")
        N2, L2, M2, particles_bf, neighbors_bf = load_data("particlesbruteForce.txt", "neighborsbruteForce.txt")
    except FileNotFoundError as e:
        print("Error: Alguno de los archivos necesarios no fue encontrado.")
        print(e)
        return

    assert N1 == N2 and L1 == L2 and M1 == M2, "Los parámetros de las simulaciones no coinciden."

    N, L, M = N1, L1, M1
    fig, ax = plt.subplots(figsize=(8, 8))
    ax.set_xlim(0, L)
    ax.set_ylim(0, L)
    ax.set_aspect('equal', adjustable='box')

    cell_size = L / M
    for i in range(M + 1):
        pos = i * cell_size
        ax.axhline(pos, color='lightgray', linestyle='--', linewidth=0.5)
        ax.axvline(pos, color='lightgray', linestyle='--', linewidth=0.5)

    # Dibujar partículas
    for p_id, data in particles_cim.items():
        circle = patches.Circle((data['x'], data['y']), radius=data['r'],
                                facecolor='skyblue', edgecolor='black', linewidth=0.75, zorder=10)
        ax.add_patch(circle)

    # Unificar representación de vecinos como pares ordenados únicos
    def get_pairs(neighbors):
        pairs = set()
        for p_id, nbrs in neighbors.items():
            for n in nbrs:
                pair = tuple(sorted((p_id, n)))
                pairs.add(pair)
        return pairs

    cim_pairs = get_pairs(neighbors_cim)
    bf_pairs = get_pairs(neighbors_bf)

    common = cim_pairs & bf_pairs
    only_cim = cim_pairs - bf_pairs
    only_bf = bf_pairs - cim_pairs

    # Dibujar vecinos
    def draw_links(pairs, color, label):
        for (p1, p2) in pairs:
            x1, y1 = particles_cim[p1]['x'], particles_cim[p1]['y']
            x2, y2 = particles_cim[p2]['x'], particles_cim[p2]['y']
            ax.plot([x1, x2], [y1, y2], color=color, linestyle='-', linewidth=1.3, label=label, zorder=1)

    draw_links(common, 'red', 'Ambos métodos')
    draw_links(only_bf, 'blue', 'Solo Brute Force')
    draw_links(only_cim, 'green', 'Solo CIM')

    handles = [
        patches.Patch(color='red', label='Ambos'),
        patches.Patch(color='blue', label='Solo BruteForce'),
        patches.Patch(color='green', label='Solo CIM')
    ]
    ax.legend(handles=handles, loc='upper right')
    ax.set_title(f'Comparación CIM vs BruteForce (N={N}, M={M}x{M})')
    ax.set_xlabel('Posición X')
    ax.set_ylabel('Posición Y')
    plt.grid(False)
    plt.show()


if __name__ == '__main__':
    visualize_comparison()
