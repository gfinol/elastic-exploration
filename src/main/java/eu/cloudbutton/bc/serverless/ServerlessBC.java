package eu.cloudbutton.bc.serverless;

import crucial.execution.ServerlessExecutorService;
import crucial.execution.aws.AWSLambdaExecutorService;
import eu.cloudbutton.bc.*;
import eu.cloudbutton.bc.multithread.CmdLineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

public class ServerlessBC {
    private Graph graph;
    public int N;
    public int M;
    public int[] verticesToWorkOn;
    public DoubleAdder[] betweennessMap;
    public AtomicLong count = new AtomicLong(0);
    public long refTime;
    public double accTime;
    public long refTs;

    private RMat rmat;
    private int permute;
    private int g;

    private static ExecutorService localExecutorService;
    private static ServerlessExecutorService serverlessExecutorService;

    static List<TaskStats> taskStatsList = Collections.synchronizedList(new ArrayList<>());

    //public AtomicInteger launchedFunctions = new AtomicInteger(0);
    // public AtomicInteger returnedFunctions = new AtomicInteger(0);

    // Constructor
    public ServerlessBC(RMat rmat, int permute, int g) {
        this.rmat = rmat;
        this.permute = permute;

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


    class LocalCallable implements Callable<Object> {
        private int startIndex;
        private int endIndex;
        private RMat rmat;
        private int permute;

        public LocalCallable(int startIndex, int endIndex, RMat rmat, int permute){
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.rmat = rmat;
            this.permute = permute;
        }

        @Override
        public Object call() throws Exception {
            long invokeTs = System.currentTimeMillis();
            Future<Result> f = serverlessExecutorService.submit(
                    new ServerlessCallable(startIndex, endIndex, rmat, permute));
            //System.out.println("Submitted " + startIndex);
            Result result = f.get();
            double[] bMapLocal = result.getBetweennessMap();
            //int countNotZeros = 0;
            for(int i=0;i<N;i++) {
                if (bMapLocal[i] != 0) {
                    //countNotZeros++;
                    //betweennessMap[i] += bMapLocal[i];
                    betweennessMap[i].add(bMapLocal[i]);
                }
            }
            long resultTs = System.currentTimeMillis();
            taskStatsList.add(new TaskStats(
                    result.getInitTs() - refTs,
                    result.getEndTs() - result.getInitTs(),
                    invokeTs - refTs,
                    resultTs - refTs
                    ));
            //System.out.println("countNotZeros = " + countNotZeros);
            //System.out.println("future.get() " + startIndex);
            //returnedFunctions.addAndGet(1);

            return null;
        }
    }


    private void runParallel() {
        refTime = System.nanoTime();
        refTs = System.currentTimeMillis();
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < N; i = i + g) {
            //bc.bfsShortestPath(i);

            /*while(launchedFunctions.get() - returnedFunctions.get() > 1000){ // FIXME: necessary, now?
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/

            //launchedFunctions.addAndGet(1);
            Future<Object> f = localExecutorService.submit(new LocalCallable(i, i + g - 1, rmat, permute));
            futures.add(f);
            //System.out.println("Submitted local " + i);
        }

        for (Future<Object> f : futures){
            try {
               f.get();
               // System.out.println("future.get() local");
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

    private static void printActualComputeTime() {
        long actualTime = 0L;
        for (TaskStats stats : taskStatsList) {
            actualTime += stats.getDuration();
        }
        System.out.println("Actual compute time: " + actualTime);

    }

    public static void main(String[] args) {
        final CmdLineOptions opts = CmdLineOptions.makeOrExit(args);
        //printUsedOptions(opts);

        try {
            System.out.println("workers set to " + opts.parallelism);
            System.out.println("g set to " + opts.g);
            serverlessExecutorService = new AWSLambdaExecutorService();
            serverlessExecutorService.setLogs(false);

            localExecutorService = Executors.newFixedThreadPool(opts.parallelism);

            long setupTime = -System.nanoTime();
            ServerlessBC bc = new ServerlessBC(new RMat(opts.seed, opts.n, opts.a, opts.b, opts.c, opts.d), opts.permute, opts.g);
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


                PlotData.plotConcurrency(taskStatsList);
                try {
                    PlotData.toCsv(taskStatsList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("refTs = " + bc.refTs);
            System.out.println("Seq N: " + bc.N + "  Setup: " + setupTime / 1e9 + "s  Processing: " + procTime / 1e9 + "s");
            System.out.println(serverlessExecutorService.printCostReport());
            printActualComputeTime();
        } finally {
            localExecutorService.shutdown();
            serverlessExecutorService.shutdown();
            System.out.println("finish");
        }
    }



}
