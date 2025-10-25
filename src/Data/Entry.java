package Data;

public class Entry<K, V> {
    public K key;
    public V value;
    public boolean isDeleted;

    public Entry(K key, V value) {
        this.key = key;
        this.value = value;
        this.isDeleted = false;
    }

    public K getKey(){
        return this.key;
    }

    public V getValue(){
        return this.value;
    }
}
