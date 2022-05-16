package eu.cloudbutton.bc;

public class TaskStats {
    private long initTime;
    private long duration;
    private long invokeTime;
    private long resultTime;

    public TaskStats(long initTime, long duration, long invokeTime, long resultTime) {
        this.initTime = initTime;
        this.duration = duration;
        this.invokeTime = invokeTime;
        this.resultTime = resultTime;
    }

    public long getInitTime() {
        return initTime;
    }

    public void setInitTime(long initTime) {
        this.initTime = initTime;
    }

    public long getEndTime() {
        return initTime + duration;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getInvokeTime() {
        return invokeTime;
    }

    public long getResultTime() {
        return resultTime;
    }

    public String toString(){
        return initTime + " (" + duration + ")";
    }
}
