package defensivebot.datasturctures;

public class CustomSet<T> {

    private LinkedList<T>[] table;
    private int capacity;
    private int itrIdx = 0;
    private Node<T> cur = null;
    private int size=0;

    public CustomSet(int capacity){
        table = new LinkedList[capacity];
        for(int i=capacity;--i>=0;)table[i] = new LinkedList<>();
        this.capacity = capacity;
    }

    public void add(T val){
        if(!contains(val)){
            table[val.hashCode()%capacity].add(val);
            size++;
        }
    }

    public void remove(T val){
        if(table[val.hashCode()%capacity].remove(val))size--;
    }

    public boolean contains(T val){
        return table[val.hashCode()%capacity].contains(val);
    }

    // iteration code
    public void initIteration(){
        itrIdx = 0;
        cur = null;
    }

    // iteration is such that even during iteration we can call remove, and it won't be a problem
    public T next(){
        if(itrIdx>=capacity)return null;
        if(cur == null || cur.next == null){
            if(cur != null)itrIdx++;
            while(itrIdx<capacity && table[itrIdx].size == 0)itrIdx++;
            if(itrIdx>=capacity)return null;
            cur = table[itrIdx].head;
        }else{
            cur = cur.next;
        }
        return cur.val;
    }


}
