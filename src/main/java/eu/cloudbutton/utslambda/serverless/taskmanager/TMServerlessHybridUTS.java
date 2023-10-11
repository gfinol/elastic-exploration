package eu.cloudbutton.utslambda.serverless.taskmanager;

import com.amazonaws.regions.Regions;
import crucial.execution.ServerlessExecutorService;
import crucial.execution.ServerlessHybridExecutorService;
import crucial.execution.aws.AWSLambdaExecutorService;
import crucial.execution.aws.AWSLambdaHybridExecutorService;
import crucial.execution.aws.Config;
import eu.cloudbutton.utslambda.Bag;
import eu.cloudbutton.utslambda.CmdLineOptions;
import eu.cloudbutton.utslambda.Utils;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author Gerard
 */
public class TMServerlessHybridUTS {
    

    public static long run(final int parallelism, final int numberOfIterationsPerWave, final int depth) {
        refTs = System.currentTimeMillis();
        TMServerlessHybridUTS waves = new TMServerlessHybridUTS(parallelism, numberOfIterationsPerWave);
        waves.run(depth);
        refEndTs = System.currentTimeMillis();

        return waves.counter.get();
    }

    private TMServerlessHybridUTS(final int parallelism, final int numberOfIterationsPerWave) {

        this.parallelism = parallelism;
        this.numberOfIterationsPerWave = numberOfIterationsPerWave;
        this.counter = new AtomicLong(0);
        this.activeThreads = new AtomicLong(0);
    }

    private static ServerlessHybridExecutorService executorService;
    private static ExecutorService localExecutorService;

    private /*final*/ int parallelism;
    private /*final*/ int numberOfIterationsPerWave;
    // private long count;
    private int step = 0;

    private Random random = new Random();

    private void run(List<Bag> bags) {

        Utils.resizeBags(bags, parallelism);
        parallelize(bags, bags.size());


        while (activeThreads.get() > 0 || !queue.isEmpty()) {
            //System.out.println(activeThreads.get());
            Bag bag = queue.poll();
            if (bag != null) {
                //System.out.println("TM: I found a bag in the queue!");
                long currentActiveThreads = activeThreads.addAndGet(-1);
                if (random.nextDouble() < 0.1) {
                    System.out.println(currentActiveThreads + " pending threads // actually " +
                            queue.size() + " bags in the queue");
                }

                // Trick
                /*if (step==0 && currentActiveThreads > 800) {
                    step++;
                    parallelism = 5;
                    numberOfIterationsPerWave = 2_500_000;
                    System.out.println("******* APPLIED STEP 0");
                }
                if (step==1 && currentActiveThreads > 1300) {
                    step++;
                    parallelism = 5;
                    numberOfIterationsPerWave = 5_000_000;
                    System.out.println("******* APPLIED STEP 1");
                }
                if (step==2 && currentActiveThreads < 1100) {
                    step++;
                    parallelism = 5;
                    numberOfIterationsPerWave = 2_500_000;
                    System.out.println("******* APPLIED STEP 2");
                }
                if (step==3 && currentActiveThreads < 100) {
                    step++;
                    parallelism = 5;
                    numberOfIterationsPerWave = 1_000_000;
                    System.out.println("******* APPLIED STEP 3");
                }*/
                
                
                /*if (currentActiveThreads > 1000-parallelism) {
                    System.out.println("Requeueing bag. Current active threads = " + currentActiveThreads);
                    queue.offer(bag);
                } else {*/

                Bag resultBag = coalesceAndCount(bag);
                if (resultBag != null) {
                    int parallelismParam = parallelism;
                    /*if (currentActiveThreads > 1000) {
                        parallelismParam = 2;
                    }*/
                    List<Bag> bags2 = new ArrayList<>();
                    bags2.add(resultBag);
                    Utils.resizeBags(bags2, parallelismParam);
                    parallelize(bags2, bags2.size());
                }
                //}
            }
        }

    }

    private void run(final int depth) {
        final Bag initBag = new Bag(64);
        final MessageDigest md = Utils.encoder();
        initBag.seed(md, 19, depth);
        initBag.bagId = UUID.randomUUID(); // for logging purposes

        final List<Bag> bags = new ArrayList<Bag>();
        bags.add(initBag);
        run(bags);
    }

    AtomicLong counter;
    AtomicLong activeThreads;
    Deque<Bag> queue = new ConcurrentLinkedDeque<>();
    //static List<Long> taskDurations = Collections.synchronizedList(new ArrayList<>());
    //static List<Map.Entry<Long, Long>> finishTimes = Collections.synchronizedList(new ArrayList<>());
    static List<TaskStats> taskStatsList = Collections.synchronizedList(new ArrayList<>());
    static Long refTs;
    static Long refEndTs;

    class LocalCallable implements Callable<Object> {

        private Bag bag;
        private int numberOfIterations;

        public LocalCallable(Bag bag, int numberOfIterations) {
            this.bag = bag;
            this.numberOfIterations = numberOfIterations;
        }

        @Override
        public Object call() throws Exception {
            Future<TMResult> future = executorService.submit(new TMBagWorkerCallable(bag, numberOfIterations));
            TMResult tmResult = future.get();
            queue.offer(tmResult.getBag());
            //taskDurations.add(tmResult.getEndTs() - tmResult.getInitTs());
            //finishTimes.add(Map.entry(tmResult.getEndTs() - refTs, tmResult.getEndTs() - tmResult.getInitTs()));
            taskStatsList.add(new TaskStats(tmResult.getBag().bagId,
                    tmResult.getBag().parentBagId,
                    tmResult.getInitTs() - refTs,
                    tmResult.getEndTs() - tmResult.getInitTs()));
            //System.out.println("-->"+tmResult.getBag().bagId+" "+tmResult.getInitTs()+"-"+tmResult.getEndTs());
            return null;
        }
    }

    public Bag coalesceAndCount(Bag b) {
        counter.addAndGet(b.count);
        b.count = 0;
        if (b.size != 0)
            return b;
        else
            return null;

    }

    public void parallelize(List<Bag> bags, int size) {

        // List<Callable<Bag>> myTasks = Collections.synchronizedList(new
        // ArrayList<>());

        activeThreads.addAndGet(size);

        //List<Future<Object>> futures = new ArrayList<>();
        for (int w = 0; w < size; w++) {
            Future<Object> future = localExecutorService
                    .submit(new LocalCallable(bags.get(w), numberOfIterationsPerWave));
            //futures.add(future);

        }

        System.out.println("Launched " + size + " remote functions...");

    }

    public static void main(String[] args) {

        final CmdLineOptions opts = CmdLineOptions.makeOrExit(args);

        try {
            System.out.println("Local workers set to " + opts.parallelism);
            executorService = new AWSLambdaHybridExecutorService(opts.parallelism); // -w option
            executorService.setLogs(false);

            localExecutorService = Executors.newFixedThreadPool(2000);

            final int parallelism = 5;
            final int numberOfIterationsPerWave = 5_000_000;

            /*
             * // Warmup phase if (opts.warmupDepth > 0) { System.out.println("Warmup...");
             * run(parallelism, numberOfIterationsPerWave, opts.warmupDepth);
             * executorService.resetCostReport(); }
             */

            System.out.println("Starting...");
            long time = -System.nanoTime();
            final long count = run(parallelism, numberOfIterationsPerWave, opts.depth);
            time += System.nanoTime();
            System.out.println("Finished.");

            System.out
                    .println("Depth: " + opts.depth + ", Performance: " + count + "/" + Utils.sub("" + time / 1e9, 0, 6)
                            + " = " + Utils.sub("" + (count / (time / 1e3)), 0, 6) + "M nodes/s");
            System.out.println(executorService.printCostReport());
            printActualComputeTime();
            //printDelayInfo();

            //PlotData.plotHistogram(taskDurations);
            //PlotData.plotScatterDuration(finishTimes);
            //PlotData.plotConcurrency(finishTimes);
            PlotData.plotConcurrency(taskStatsList);


        } finally {
            executorService.shutdown();
            localExecutorService.shutdown();
            System.out.println("finish");
        }
    }

    private static void printDelayInfo() {
        List<Long> delays = new ArrayList<>();
        /*long maxFinishTime = 0L;
        TaskStats lastTaskStats = null;

        for (TaskStats stats : taskStatsList) {
            if (stats.getInitTime()+stats.getDuration() > maxFinishTime){
                maxFinishTime = stats.getInitTime()+stats.getDuration();
                lastTaskStats = stats;
            }
        }*/

        // Sort stats by endTime reverse
        List<TaskStats> sortedTaskStatsListReverse = new ArrayList<>(taskStatsList);
        sortedTaskStatsListReverse.sort(new Comparator<TaskStats>() {
            @Override
            public int compare(TaskStats o1, TaskStats o2) {
                return Long.compare(o2.getEndTime(), o1.getEndTime());
            }
        });

        // Last delay (time between last task end and ts at client)
        TaskStats lastTaskStats = sortedTaskStatsListReverse.get(0);
        System.out.println("refEndTs-refTs:" + (refEndTs-refTs));
        System.out.println(refEndTs);
        System.out.println(refTs);
        System.out.println(lastTaskStats.getInitTime());
        System.out.println(lastTaskStats.getDuration());
        delays.add(refEndTs - refTs - lastTaskStats.getEndTime());

        long ptr = 0L;

        Set<UUID> currentIds = new HashSet<>();
        currentIds.add(lastTaskStats.getBagId());
        for (TaskStats stats : sortedTaskStatsListReverse) {
            if (currentIds.contains(stats.getBagId())) {
                System.out.println("** " + stats.toString());

                if (ptr - stats.getEndTime() > 0) {
                    delays.add(ptr - stats.getEndTime());
                }
                ptr = stats.getInitTime();
                if (stats.getParentBagId() != null) {
                    currentIds.add(stats.getParentBagId());
                }

            }
        }

        long totalDelay = 0L;
        for (Long delay : delays) {
            totalDelay += delay;
            System.out.println(delay);
        }
        System.out.println("Total delay: "+totalDelay);

    }

    private static void printActualComputeTime() {
        long actualTime = 0L;
        for (TaskStats stats : taskStatsList) {
            actualTime += stats.getDuration();
        }
        System.out.println("Actual compute time: " + actualTime);

    }
}
