package hack.echo.client.chunk;

public class DisjointUnionSets {
    byte[] rank = new byte[4096];
    int[] parent = new int[4096];

    DisjointUnionSets() {
        for (int i = 0; i < 4096; i++) {
            parent[i] = i;
        }
    }

    int find(int i) {
        int root = parent[i];
        if (parent[root] != root) {
            return parent[i] = find(root);
        }
        return root;
    }

    void union(int x, int y) {
        int xRoot = find(x);
        int yRoot = find(y);
        if (xRoot == yRoot) return;

        if (rank[xRoot] < rank[yRoot]) {
            parent[xRoot] = yRoot;
        } else if (rank[yRoot] < rank[xRoot]) {
            parent[yRoot] = xRoot;
        } else {
            parent[yRoot] = xRoot;
            rank[xRoot]++;
        }
    }
}
