package eu.cloudbutton.bc.serverless;

import eu.cloudbutton.bc.FixedArrayQueue;
import eu.cloudbutton.bc.Graph;
import eu.cloudbutton.bc.RMat;
import eu.cloudbutton.bc.Random;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class ServerlessCallable implements Callable<Result>, Serializable  {
    private int startIndex;
    private int endIndex;
    private RMat rmat;
    private int permute;

    private Graph graph;
    private int N;
    //private int M;
    private int[] verticesToWorkOn;
    private long count;

    // These are the per-vertex data structures.
    private int[] predecessorMap;
    private int[] predecessorCount;
    private long[] distanceMap;
    private long[] sigmaMap;

    public FixedArrayQueue<Integer> regularQueue;
    private double[] deltaMap;
    private double[] betweennessMapLocal;

    public ServerlessCallable(int startIndex, int endIndex, RMat rmat, int permute) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.rmat = rmat;
        this.permute = permute;
        //this.graph = graph;
        //this.verticesToWorkOn = verticesToWorkOn;
        //this.N = graph.numVertices();



    }

    private void generateGraph(){
        graph = rmat.generate();
        //System.out.println(graph.toString());
        graph.compress();
        N = graph.numVertices();
        //M = graph.numEdges();
        verticesToWorkOn = new int[N]; // Rail[Int](N, (i:Long)=>i as Int);
        for (int i = 0; i < N; i++) {
            verticesToWorkOn[i] = i;
        }
        if (permute > 0)
            permuteVertices();
    }

    private void resetArrays(){
        predecessorMap = new int[graph.numEdges()];
        predecessorCount = new int[N];
        distanceMap = new long[N];
        for (int i = 0; i < N; i++) {
            distanceMap[i] = Long.MAX_VALUE;
        }
        sigmaMap = new long[N];
        regularQueue = new FixedArrayQueue<>(N); // (N)
        deltaMap = new double[N];
        betweennessMapLocal = new double[N];
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
            betweennessMapLocal[w] += deltaMap[w];
        distanceMap[w] = Long.MAX_VALUE;
        sigmaMap[w] = 0L;
        deltaMap[w] = 0.0;
    }

    protected final void bfsShortestPath(int vertexIndex) {
        //refTime = System.nanoTime();
        int s = verticesToWorkOn[vertexIndex];
        bfsShortestPath1(s);
        while (!regularQueue.isEmpty()) {
            bfsShortestPath2();
        }
        bfsShortestPath3();
        while (!regularQueue.isEmpty()) {
            bfsShortestPath4(s);
        }
        //accTime += (System.nanoTime() - refTime) / 1e9;
    }

    @Override
    public Result call() throws Exception {
        long initTs = System.currentTimeMillis();
        generateGraph();
        resetArrays();
        //long endSetupTs = System.currentTimeMillis();
        //System.out.println("Setup graph:" + (endSetupTs-initTs));

        for(int i = startIndex; i <= endIndex; i++){
            bfsShortestPath(i);
        }
        long endTs = System.currentTimeMillis();

        Result result = new Result(betweennessMapLocal, initTs, endTs);
        return result;
    }
}
