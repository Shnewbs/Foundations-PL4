package net.foundations.pl4.core;

/** Bounded iterative union-find; no recursive traversal or world/chunk access. */
public final class DisjointSets {
    private final int[] parent, sizes;
    public DisjointSets(int size) {
        if (size < 0) throw new IllegalArgumentException("Negative set size");
        parent = new int[size]; sizes = new int[size];
        for (int i = 0; i < size; i++) { parent[i] = i; sizes[i] = 1; }
    }
    public int find(int element) {
        int root = element;
        while (root != parent[root]) root = parent[root];
        while (element != root) { int next = parent[element]; parent[element] = root; element = next; }
        return root;
    }
    public void union(int a, int b) {
        int ra = find(a), rb = find(b); if (ra == rb) return;
        if (sizes[ra] < sizes[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra; sizes[ra] += sizes[rb];
    }
}
