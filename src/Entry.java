public class Entry<K, V> {
    public K key;
    public V value;
    public boolean isDeleted;

    public Entry(K key, V value) {
        this.key = key;
        this.value = value;
        this.isDeleted = false;
    }
}
