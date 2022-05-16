package eu.cloudbutton.bc;

import eu.cloudbutton.bc.CmdLineOptions;

public class CmdLineOptions {
    
    public long seed = 2;
    public int n = 2;
    public double a = 0.55;
    public double b = 0.1;
    public double c = 0.1;
    public double d = 0.25;
    public int permute = 1;
    public int verbose = 1;

    public static CmdLineOptions makeOrExit(final String args[]) {
        try {
            return new CmdLineOptions(args);
        } catch (final IllegalArgumentException ex) {
            final String msg = ex.getMessage();
            int exitCode = 0;
            if (!msg.equals("help")) {
                System.err.println(ex.getMessage());
                exitCode = -1;
            }
            CmdLineOptions.printUsage();
            System.exit(exitCode);
            return null;
        }
    }

    public CmdLineOptions(final String[] args) {

        for (int curArg = 0; curArg < args.length; curArg++) {
            final String arg = args[curArg];

            if (arg.equalsIgnoreCase("-seed") || arg.equalsIgnoreCase("-s")) {
                curArg++;
                if (curArg >= args.length) {
                    throw new IllegalArgumentException("Illegal " + arg + " argument with no corresponding number");
                }
                final String arg2 = args[curArg];
                try {
                    seed = Long.parseLong(arg2);
                } catch (final Exception e) {
                    throw new IllegalArgumentException("seed argument is not parseable as a long " + arg2);
                }
            } else if (arg.equalsIgnoreCase("-n")) {
                curArg++;
                if (curArg >= args.length) {
                    throw new IllegalArgumentException("Illegal " + arg + " argument with no corresponding depth");
                }
                final String arg2 = args[curArg];
                try {
                    n = Integer.parseInt(arg2);
                } catch (final Exception e) {
                    throw new IllegalArgumentException("n argument is not parseable as an integer " + arg2);
                }
            } else if (arg.equalsIgnoreCase("-a")) {
                curArg++;
                if (curArg >= args.length) {
                    throw new IllegalArgumentException("Illegal " + arg + " argument with no corresponding depth");
                }
                final String arg2 = args[curArg];
                try {
                    a = Double.parseDouble(arg2);
                } catch (final Exception e) {
                    throw new IllegalArgumentException("a places argument is not parseable as a long " + arg2);
                }
            } else if (arg.equalsIgnoreCase("-b")) {
                curArg++;
                if (curArg >= args.length) {
                    throw new IllegalArgumentException("Illegal " + arg + " argument with no corresponding depth");
                }
                final String arg2 = args[curArg];
                try {
                    b = Double.parseDouble(arg2);
                } catch (final Exception e) {
                    throw new IllegalArgumentException("b argument is not parseable as a long " + arg2);
                }
            } else if (arg.equalsIgnoreCase("-c")) {
                curArg++;
                if (curArg >= args.length) {
                    throw new IllegalArgumentException("Illegal " + arg + " argument with no corresponding depth");
                }
                final String arg2 = args[curArg];
                try {
                    c = Double.parseDouble(arg2);
                } catch (final Exception e) {
                    throw new IllegalArgumentException("c argument is not parseable as a long " + arg2);
                }
            } else if (arg.equalsIgnoreCase("-d")) {
                curArg++;
                if (curArg >= args.length) {
                    throw new IllegalArgumentException("Illegal " + arg + " argument with no corresponding depth");
                }
                final String arg2 = args[curArg];
                try {
                    d = Double.parseDouble(arg2);
                } catch (final Exception e) {
                    throw new IllegalArgumentException("d argument is not parseable as a long " + arg2);
                }
            } else if (arg.equalsIgnoreCase("-permute") || arg.equalsIgnoreCase("-p")) {
                curArg++;
                if (curArg >= args.length) {
                    throw new IllegalArgumentException("Illegal " + arg + " argument with no corresponding depth");
                }
                final String arg2 = args[curArg];
                try {
                    permute = Integer.parseInt(arg2);
                } catch (final Exception e) {
                    throw new IllegalArgumentException("warmupDepth argument is not parseable as an integer " + arg2);
                }
            } else if (arg.equalsIgnoreCase("-verbose") || arg.equalsIgnoreCase("-v")) {
                curArg++;
                if (curArg >= args.length) {
                    throw new IllegalArgumentException(
                            "Illegal " + arg + " argument with no corresponding place:timespan argument");
                }
                final String arg2 = args[curArg];
                try {
                    verbose = Integer.parseInt(arg2);
                } catch (final Exception e) {
                    throw new IllegalArgumentException("verbose argument is not parseable as an integer " + arg2);
                }

            } else {
                    throw new IllegalArgumentException("Unknown argument " + arg);
            }
        }

        if (seed <= 0) {
            throw new IllegalArgumentException("seed argument must be positive, not " + seed);
        }

        if (n < 0) {
            throw new IllegalArgumentException("n argument must be non-negative, not " + n);
        }

    }

    /**
     * parses a string and returns the corresponding timespan in nanoseconds
     */
    public static long getTime(String timeString) {
        final long units;

        if (timeString.endsWith("ns")) {
            timeString = timeString.substring(0, timeString.length() - 2);
            units = 1;
        } else if (timeString.endsWith("ms")) {
            timeString = timeString.substring(0, timeString.length() - 2);
            units = 1000;
        } else if (timeString.endsWith("s")) {
            timeString = timeString.substring(0, timeString.length() - 1);
            units = 1000 * 1000;
        } else if (timeString.endsWith("m")) {
            timeString = timeString.substring(0, timeString.length() - 1);
            units = 1000 * 1000 * 60;
        } else if (timeString.endsWith("h")) {
            timeString = timeString.substring(0, timeString.length() - 1);
            units = 1000 * 1000 * 60 * 60;
        } else {
            units = 1;
        }
        return Long.parseLong(timeString) * units;
    }
    
    static void printUsage() {
        System.err.println("invoked as BC ARGS where ARGS can be from");
        //System.err.println("-help\t\tPrint this usage message and quit");

        System.err.println("-s <LONG>\t\tSet the the seed for the random number");
        System.err.println("-n <INT>\t\tSet the number of vertices = 2^n");
        System.err.println("-a <DOUBLE>\t\tSet the probability a");
        System.err.println("-b <DOUBLE>\t\tSet the probability b");
        System.err.println("-c <DOUBLE>\t\tSet the probability c");
        System.err.println("-d <DOUBLE>\t\tSet the probability d");
        System.err.println("-p <INT>\t\tSet the permutation to be used");
        System.err.println("-v <INT>\t\tSet the verbosity to be used");
    }


}
