package se.kth.news.core.paxos.messages;

/**
 * Created by Love on 2016-05-18.
 */
public class Prepare<T> {

    private T value;
    private int ballot;

    public Prepare(T value, int ballot) {
        this.value = value;
        this.ballot = ballot;
    }

    public T getValue() {
        return value;
    }

    public int getBallot() {
        return ballot;
    }
}
