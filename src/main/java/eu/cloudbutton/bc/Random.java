package eu.cloudbutton.bc;

import java.util.concurrent.atomic.AtomicLong;

/** 
 * Generate pseudo-random numbers.
 *
 * The underlying pseudo-random stream is generated 
 * using the SplittableRandom algorithm described by Steele, Lea, and Flood
 * in "Fast Splittable Pseudorandom Number Generators", OOPSLA 2014.
 */
public final class Random {
    private static AtomicLong defaultGen = new AtomicLong(System.nanoTime());
    private static long GOLDEN_GAMMA = 0x9e37_79b9_7f4a_7c15L;
    private static float FLOAT_ULP = 1.0f / (1 << 24);
    private static double DOUBLE_ULP = 1.0 / (1L << 53);

    private long seed;
    private long gamma;
    private double storedGaussian;
    private boolean haveStoredGaussian = false;

    private Random(long seed, long  gamma) {
        this.seed = seed;
        this.gamma = gamma;
    }

    public Random(long seed) {
        this(seed, GOLDEN_GAMMA);
    }

    public Random() {
        long s = defaultGen.getAndAdd(2 * GOLDEN_GAMMA);
        seed = mix64(s);
        gamma = mixGamma(s + GOLDEN_GAMMA);
    }

    /** Split and return a new Random instance derived from this one */
    public Random split() {
        return new Random(mix64(nextSeed()), mixGamma(nextSeed()));
    }
    
     
    /** Return a 32-bit random integer */
    public int nextInt() {
        return mix32(nextSeed());
    }

    /** Return a 32-bit random integer in the range 0 to maxPlus1-1
     * when maxPlus1 > 0. Return 0 if maxPlus1 <= 0 instead of throwing 
     * an IllegalArgumentException, to simplify user code.
     */
    public int nextInt(int maxPlus1) {
        if (maxPlus1 <= 0)
            return 0;
        
        int n = maxPlus1;

        if ((n & -n) == n) {
            // If a power of 2, just mask nextInt
            return nextInt() & (n-1);
        }

        int mask = 1;
        while ((n & ~mask) != 0) {
            mask <<= 1;
            mask |= 1;
        }

        // Keep generating numbers of the right size until we get
        // one in range.  The expected number of iterations is 2.
        int x;

        do {
            x = nextInt() & mask;
        } while (x >= n);

        return x;
    }

    public void nextBytes(byte[] buf) {
        int i = 0;
        while (true) {
            long x = nextLong();
            for (i = 0; i < 8; i++) {
                if (i >= buf.length)
                    return;
                buf[i] = (byte)(x & 0xff);
                i++;
                x >>= 8;
            }
        }
    }
     
    /** Return a 64-bit random (Long) integer */
    public long nextLong() {
        return mix64(nextSeed());
    }

    public long nextLong(long maxPlus1) {
        if (maxPlus1 <= 0)
            return 0;
        
        long n = maxPlus1;

        if ((n & -n) == n) {
            // If a power of 2, just mask nextInt
            return nextLong() & (n-1);
        }

        long mask = 1;
        while ((n & ~mask) != 0) {
            mask <<= 1;
            mask |= 1;
        }

        // Keep generating numbers of the right size until we get
        // one in range.  The expected number of iterations is 2.
        long x;

        do {
            x = nextLong() & mask;
        } while (x >= n);

        return x;
    }

    /** Return a random boolean. */
    public boolean nextBoolean() {
        return nextInt() < 0;
    }

    /** Return a random float between 0.0f and 1.0f. */
    public float nextFloat() {
        return (nextInt() >>> 8) * FLOAT_ULP;
    }

    /** Return a random double between 0.0 and 1.0. */
    public double nextDouble() {
        return (nextLong() >>> 11) * DOUBLE_ULP;
    }

    /**
     * Generate a pseudo-random number from a Gaussian distribution with a mean
     * of 0 and a standard deviation of 1, using the polar form of the
     * Box-Muller transform.
     * @return a random sample from a standard normal distribution
     * @see Knuth (1981) "The Art of Computer Programming, Volume 2: Seminumerical Algorithms"
     */
    public double nextGaussian() {
        if (haveStoredGaussian) {
            haveStoredGaussian = false;
            return storedGaussian;
        } else {
            double u1;
            double u2;
            double s;
            do {
                u1 = 2.0 * nextDouble() - 1.0;
                u2 = 2.0 * nextDouble() - 1.0;
                s = u1 * u1 + u2 * u2;
            } while (s >= 1.0 || s == 0.0);
            double m = Math.sqrt(-2.0 * Math.log(s) / s);
            storedGaussian = u2 * m;
            haveStoredGaussian = true;
            return u1 * m;
        }
    }

    private long nextSeed() {
        return (seed += gamma);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    private static int mix32(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return (int)(z >>> 32); 
    }

    private static long mix64variant13(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31); 
    }

    private static long mixGamma(long z) {
        z = mix64variant13(z) | 1;
        int n = Long.bitCount(z ^ (z >>> 1));
        if (n >= 24) { 
            z ^= 0xaaaaaaaaaaaaaaaaL;
        }
        return z; 
    }
}
