package se.kth.news.core.paxos.messages;

/**
 * Created by Love on 2016-05-18.
 */
public class Accept<T> {

    private T value;
    private int ballot = -1;

    public Accept(T value) {
        this.value = value;
    }

    public Accept(T value, int ballot) {
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
