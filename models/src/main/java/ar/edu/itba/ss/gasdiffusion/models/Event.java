package ar.edu.itba.ss.gasdiffusion.models;

public abstract class Event implements Comparable<Event> {
    final double time; // Time has to go here because we need that all the implementations of the Comparable to be equal

    /* Package Private */
    Event(double time) {
        this.time = time;
    }

    @Override
    public int compareTo(final Event otherEvent) {
        if (this.time < otherEvent.time) {
            return -1;
        }
        if (this.time > otherEvent.time) {
            return 1;
        }
        else return 0;
    }

    public double getTime() {
        return time;
    }
}
