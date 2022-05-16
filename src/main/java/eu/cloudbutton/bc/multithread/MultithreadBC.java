package eu.cloudbutton.bc.multithread;

import eu.cloudbutton.bc.FixedArrayQueue;
import eu.cloudbutton.bc.Graph;
import eu.cloudbutton.bc.RMat;
import eu.cloudbutton.bc.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

public class MultithreadBC {
    private Graph graph;
    public int N;
    public int M;
    public int[] verticesToWorkOn;
    //public double[] betweennessMap;
    public DoubleAdder[] betweennessMap;
    public AtomicLong count = new AtomicLong(0);
    public long refTime;
    public double accTime;

    public int g;

    private static ExecutorService localExecutorService;

    // Constructor
    public MultithreadBC(RMat rmat, int permute, int g) {
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
        //betweennessMap = new double[N];
        betweennessMap = new DoubleAdder[N];
        for (int i = 0; i < N; i++) {
            betweennessMap[i] = new DoubleAdder();
        }

        this.g = g;
    }


    class LocalCallable implements Callable<Object> {
        int startIndex;
        int endIndex;
        //MultithreadBC bc;

        // These are the per-vertex data structures.
        private int[] predecessorMap;
        private int[] predecessorCount;
        private long[] distanceMap;
        private long[] sigmaMap;

        public FixedArrayQueue<Integer> regularQueue;
        private double[] deltaMap;
        private double[] betweennessMapLocal;

        public LocalCallable(int startIndex, int endIndex ){
            this.startIndex = startIndex;
            this.endIndex = endIndex;

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

        protected final void bfsShortestPath1(int s) {
            // Put the values for source vertex
            distanceMap[s] = 0L;
            sigmaMap[s] = 1L;
            regularQueue.push(s); // add to tail
        }

        protected final void bfsShortestPath2() {
            //count.addAndGet(1);
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
        public Object call() throws Exception {
            for(int i = startIndex; i <= endIndex; i++){
                bfsShortestPath(i);
            }

            // Update global map
            for(int i=0;i<N;i++) {
                if (betweennessMapLocal[i] != 0) {
                    betweennessMap[i].add(betweennessMapLocal[i]);
                }
            }
            //return betweennessMapLocal;
            return null;
        }
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
            if (betweennessMap[i].doubleValue() != 0.0) {
                System.out.println("(" + i + ") -> " + betweennessMap[i].doubleValue());
            }
        }
    }

    /**
     * Dump the betweenness map.
     *
     * @param numDigit number of digits to print
     */
    private void printBetweennessMap(int numDigit) {
        for (int i = 0; i < N; ++i) {
            if (betweennessMap[i].doubleValue() != 0.0) {
                System.out.println("(" + i + ") -> " + sub("" + betweennessMap[i].doubleValue(), 0, numDigit));
            }
        }
    }

    /**
     * Dump the first n items of the betweenness map.
     *
     * @param numDigit number of digits to print
     */
    private void printBetweennessMap(int numDigit, int items) {
        for (int i = 0; i < items; ++i) {
            if (betweennessMap[i].doubleValue() != 0.0) {
                System.out.println("(" + i + ") -> " + sub("" + betweennessMap[i].doubleValue(), 0, numDigit));
            }
        }
    }

    /**
     * substring helper function
     */
    public static String sub(String str, int start, int end) {
        return str.substring(start, Math.min(end, str.length()));
    }



    private void runParallel() {
        refTime = System.nanoTime();
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < N; i = i + g) {
            //bc.bfsShortestPath(i);
            Future<Object> f = localExecutorService.submit(new LocalCallable(i, i + g - 1));
            futures.add(f);
            //System.out.println("Submitted " + i);
        }

        for (Future<Object> f : futures){
            try {
                //double[] bMapLocal = (double[])f.get();
                //for(int i=0;i<N;i++)
                //    betweennessMap[i] += bMapLocal[i];
                f.get();
                //System.out.println("future.get()");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        accTime += (System.nanoTime() - refTime) / 1e9;
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
        //printUsedOptions(opts);

        try {
            System.out.println("workers set to " + opts.parallelism);
            System.out.println("g set to " + opts.g);
            localExecutorService = Executors.newFixedThreadPool(opts.parallelism);

            long setupTime = -System.nanoTime();
            MultithreadBC bc = new MultithreadBC(new RMat(opts.seed, opts.n, opts.a, opts.b, opts.c, opts.d), opts.permute, opts.g);
            setupTime += System.nanoTime();

            long procTime = -System.nanoTime();
            bc.runParallel();
            procTime += System.nanoTime();

            if (opts.verbose > 0) {
                System.out.println(
                        "Time = " + bc.accTime + "s Count = " + bc.count);
            }

            if (opts.verbose > 2) {
                System.out.println("**Betweenness map**");
                bc.printBetweennessMap(6, 10);
            }

            System.out.println("Seq N: " + bc.N + "  Setup: " + setupTime / 1e9 + "s  Processing: " + procTime / 1e9 + "s");
        } finally {
            localExecutorService.shutdown();
            System.out.println("finish");
        }
    }



}
