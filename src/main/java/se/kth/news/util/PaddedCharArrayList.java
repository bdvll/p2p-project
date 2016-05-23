package se.kth.news.util;

import java.util.Arrays;

/**
 * Created by Love on 2016-05-20.
 */
public class PaddedCharArrayList {

    private static final int START_SIZE = 16;

    private char[] list;
    private char padding;
    private int size = START_SIZE;
    private int currentIndex = 0;

    public PaddedCharArrayList(){
        list = new char[size];
    }

    public PaddedCharArrayList(char padding){
        list = new char[size];
        this.padding = padding;
        pad(0, size);
    }

    private void pad(int from, int to){
        for(int i = from; i < to; ++i){
            list[i] = padding;
        }
    }

    private void extend(int newSize){
        size = newSize;
        list = Arrays.copyOf(list, size);
        pad(size/2, size);
    }

    public void add(char element){
        if(currentIndex == size)
            extend(size*2);
        list[currentIndex++] = element;
    }

    public void add(int index, char element){
        if(index >= size)
            extend(powAbove(index));
        list[index] = element;
        currentIndex = index + 1;
    }

    public char get(int index){
        if(index >= size)
            return padding;
        return list[index];
    }

    private int powAbove(int index){
        int i = START_SIZE;
        while(i <= index){
            i *= 2;
        }
        return i;
    }

    public int size(){
        return size;
    }


    @Override
    public String toString() {
        return new String(list);
    }

}
