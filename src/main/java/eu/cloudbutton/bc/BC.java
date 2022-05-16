package eu.cloudbutton.bc;

public class BC {
    private Graph graph;
    public int N;
    public int M;
    public int[] verticesToWorkOn;
    public double[] betweennessMap;
    public long count = 0;
    public long refTime;
    public double accTime;

    // These are the per-vertex data structures.
    private int[] predecessorMap;
    private int[] predecessorCount;
    private long[] distanceMap;
    private long[] sigmaMap;

    // public val regularQueue:FixedRailQueue[Int];
    public FixedArrayQueue<Integer> regularQueue;
    private double[] deltaMap;

    // Constructor
    public BC(RMat rmat, int permute) {
        graph = rmat.generate();
        //System.out.println(graph.toString());
        graph.compress();
        N = graph.numVertices();
        M = graph.numEdges();
        verticesToWorkOn = new int[N]; // Rail[Int](N, (i:Long)=>i as Int);
        for (int i = 0; i < N; i++) {
            verticesToWorkOn[i] = i;
        }
        if (permute > 0)
            permuteVertices();
        betweennessMap = new double[N];

        predecessorMap = new int[graph.numEdges()];
        predecessorCount = new int[N];
        distanceMap = new long[N]; // Rail[Long](N, Long.MAX_VALUE);
        for (int i = 0; i < N; i++) {
            distanceMap[i] = Long.MAX_VALUE;
        }
        sigmaMap = new long[N];
        regularQueue = new FixedArrayQueue<>(N); // (N)
        deltaMap = new double[N];
    }

    /**
     * A function to shuffle the vertices randomly to give better work dist.
     */
    private void permuteVertices() {
        Random prng = new Random(1);

        for (int i = 0; i < N; i++) {
            int indexToPick = prng.nextInt(N - i);
            int tmp = verticesToWorkOn[i];
            verticesToWorkOn[i] = verticesToWorkOn[i + indexToPick];
            verticesToWorkOn[i + indexToPick] = tmp;
        }
    }

    /**
     * Dump the betweenness map.
     */
    public void printBetweennessMap() {
        for (int i = 0; i < N; ++i) {
            if (betweennessMap[i] != 0.0) {
                System.out.println("(" + i + ") -> " + betweennessMap[i]);
            }
        }
    }

    /**
     * substring helper function
     */
    public static String sub(String str, int start, int end) {
        return str.substring(start, Math.min(end, str.length()));
    }

    protected final void bfsShortestPath1(int s) {
        // Put the values for source vertex
        distanceMap[s] = 0L;
        sigmaMap[s] = 1L;
        regularQueue.push(s); // add to tail
    }

    protected final void bfsShortestPath2() {
        count++;
        // Pop the node with the least distance
        int v = regularQueue.pop(); // remove from head

        // Get the start and the end points for the edge list for "v"
        int edgeStart = graph.begin(v);
        int edgeEnd = graph.end(v);

        // Iterate over all its neighbors
        for (int wIndex = edgeStart; wIndex < edgeEnd; wIndex++) {
            // Get the target of the current edge.
            int w = graph.getAdjacentVertexFromIndex(wIndex);
            long distanceThroughV = distanceMap[v] + 1L;

            // In BFS, the minimum distance will only be found once --- the
            // first time that a node is discovered. So, add it to the queue.
            if (distanceMap[w] == Long.MAX_VALUE) {
                regularQueue.push(w); // add to tail
                distanceMap[w] = distanceThroughV;
            }

            // If the distance through "v" for "w" from "s" was the same as its
            // current distance, we found another shortest path. So, add
            // "v" to predecessorMap of "w" and update other maps.
            if (distanceThroughV == distanceMap[w]) {
                sigmaMap[w] = sigmaMap[w] + sigmaMap[v];// XTENLANG-2027
                predecessorMap[graph.rev(w) + predecessorCount[w]++] = v;
            }
        }
    }

    protected final void bfsShortestPath3() {
        regularQueue.rewind();
    }

    protected final void bfsShortestPath4(int s) {
        int w = regularQueue.top(); // remove from tail
        int rev = graph.rev(w);
        while (predecessorCount[w] > 0) {
            int v = predecessorMap[rev + --predecessorCount[w]];
            deltaMap[v] += (((double) sigmaMap[v]) / sigmaMap[w]) * (1.0 + deltaMap[w]);
        }

        // Accumulate updates locally
        if (w != s)
            betweennessMap[w] += deltaMap[w];
        distanceMap[w] = Long.MAX_VALUE;
        sigmaMap[w] = 0L;
        deltaMap[w] = 0.0;
    }

    protected final void bfsShortestPath(int vertexIndex) {
        refTime = System.nanoTime();
        int s = verticesToWorkOn[vertexIndex];
        bfsShortestPath1(s);
        while (!regularQueue.isEmpty()) {
            bfsShortestPath2();
        }
        bfsShortestPath3();
        while (!regularQueue.isEmpty()) {
            bfsShortestPath4(s);
        }
        accTime += (System.nanoTime() - refTime) / 1e9;
    }

    /**
     * Dump the betweenness map.
     * 
     * @param numDigit number of digits to print
     */
    private void printBetweennessMap(int numDigit) {
        for (int i = 0; i < N; ++i) {
            if (betweennessMap[i] != 0.0) {
                System.out.println("(" + i + ") -> " + sub("" + betweennessMap[i], 0, numDigit));
            }
        }
    }

    public static void printUsedOptions(CmdLineOptions opts) {
        System.out.println("Running BC with the following parameters:");
        System.out.println("seed = " + opts.seed);
        System.out.println("N = " + (1 << opts.n));
        System.out.println("a = " + opts.a);
        System.out.println("b = " + opts.b);
        System.out.println("c = " + opts.c);
        System.out.println("d = " + opts.d);
    }

    public static void main(String[] args) {
        final CmdLineOptions opts = CmdLineOptions.makeOrExit(args);
        printUsedOptions(opts);

        long setupTime = -System.nanoTime();
        BC bc = new BC(new RMat(opts.seed, opts.n, opts.a, opts.b, opts.c, opts.d), opts.permute);
        setupTime += System.nanoTime();

        long procTime = -System.nanoTime();
        for (int i = 0; i < bc.N; i++) {
            bc.bfsShortestPath(i);
        }
        procTime += System.nanoTime();

        if (opts.verbose > 0) {
            System.out.println(
                    "Time = " + bc.accTime + "s Count = " + bc.count);
        }

        if (opts.verbose > 2) {
            System.out.println("**Betweenness map**");
            bc.printBetweennessMap(6);
        }

        System.out.println("Seq N: " + bc.N + "  Setup: " + setupTime / 1e9 + "s  Processing: " + procTime / 1e9 + "s");

    }

}
