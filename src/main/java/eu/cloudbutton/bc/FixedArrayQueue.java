package eu.cloudbutton.bc;

import java.io.Serializable;

public final class FixedArrayQueue<T> implements Serializable {

    private Object[] internalStorage;
    private int N;
    private int head;
    private int tail;

    /** Construct a fixed size queue */
    public FixedArrayQueue(int N) {
        this.N = N;
        this.internalStorage = new Object[N];
        this.head = 0;
        this.tail = 0;
    }

    /** Check if the queue is empty */
    public boolean isEmpty() {
        return this.head == this.tail;
    }

    /** Add the element to the front of the queue. */
    public void push(T v) {
        // Add the element and increase the size
        this.internalStorage[this.tail++] = v;
    }

    /** Remove and return one element of the queue if FIFO order. */
    public T pop() {
        // Remove the first element from the queue.
        return (T) this.internalStorage[this.head++];
    }

    /** Remove and return one element of the queue in LIFO order. */
    public T top() {
        return (T) this.internalStorage[--this.tail];
    }

    /** Rewind. */
    public void rewind() {
        this.head = 0;
    }

    /** Output the contents of the queue in the order they are stored */
    public void print() {
        System.out.println("h = " + head + ", t = " + tail + ", ");
        System.out.print("[");
        for (int i = head; i < tail; i++) {
            System.out.print(((i == head) ? "" : ",") + this.internalStorage[i]);
        }
        System.out.println("]");
    }

    public static void main(String[] args) {
        FixedArrayQueue<Integer> myQueue = new FixedArrayQueue<>(4);

        myQueue.push(1);
        myQueue.push(2);
        myQueue.push(3);
        myQueue.push(4);
        myQueue.print();

        myQueue.pop();
        myQueue.pop();
        myQueue.print();

        myQueue.pop();
        myQueue.pop();
        myQueue.print();

        myQueue.push(5);
        myQueue.print();

        myQueue.push(2);
        myQueue.push(3);
        myQueue.push(4);
        myQueue.print();
    }
}
