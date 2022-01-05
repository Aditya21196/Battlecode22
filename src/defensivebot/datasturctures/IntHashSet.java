package defensivebot.datasturctures;

public class IntHashSet {

    private LinkedList<Integer>[] table;
    private int capacity;

    IntHashSet(int capacity){
        table = new LinkedList[capacity];
        for(int i=capacity;--i>=0;)table[i] = new LinkedList<>();
        this.capacity = capacity;
    }

    public void add(int val){
        if(!contains(val))table[val%capacity].add(val);
    }

    public void remove(int val){
        table[val%capacity].remove(val);
    }

    public boolean contains(int val){
        return table[val%capacity].contains(val);
    }

    // iteration code


}
