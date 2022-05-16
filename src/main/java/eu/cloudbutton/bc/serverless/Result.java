package eu.cloudbutton.bc.serverless;

import java.io.Serializable;

public class Result implements Serializable {
    private double[] betweennessMap;
    private long initTs;
    private long endTs;

    public Result(double[] betweennessMap, long initTs, long endTs) {
        this.betweennessMap = betweennessMap;
        this.initTs = initTs;
        this.endTs = endTs;
    }

    public double[] getBetweennessMap() {
        return betweennessMap;
    }

    public long getInitTs() {
        return initTs;
    }

    public long getEndTs() {
        return endTs;
    }

}
