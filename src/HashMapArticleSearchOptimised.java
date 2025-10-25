import java.util.Objects;

public class HashMapArticleSearchOptimised<Integer,Article> {

    public enum HashFunctionType {SSF, PAF}

    public enum CollisionType {LINEAR_PROBING, DOUBLE_HASHING}

    private static final int DEFAULT_INITIAL_CAPACITY = 30000;
    private static final double DEFAULT_LOAD_FACTOR = 0.5;
    private static final int PAF_Z = 33;

    private Entry<Integer,Article>[] table;
    private int size;
    private double loadFactor;
    private HashFunctionType hashFunctionType;
    private CollisionType collisionType;
    private long collisionCount;
    private int currentCapacityPrime;

    public HashMapArticleSearchOptimised(int initialCapacity, double loadFactor,
                         HashFunctionType hashFunctionType, CollisionType collisionType) {

        this.currentCapacityPrime = nextPrime(Math.max(3, initialCapacity));
        this.table = (Entry<Integer,Article>[]) new Entry[this.currentCapacityPrime];
        this.size = 0;
        this.loadFactor = loadFactor;
        this.hashFunctionType = hashFunctionType;
        this.collisionType = collisionType;
        this.collisionCount = 0;
    }

    public HashMapArticleSearchOptimised() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, HashFunctionType.PAF, CollisionType.LINEAR_PROBING);
    }

    public long getCollisionCount() {
        return collisionCount;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(Integer key) {
        return get(key) != null;
    }

    public Article get(Integer key) {
        int index = findSlotIndex(key, false); // false -> don't insert, just find
        if (index == -1) return null;
        Entry<Integer,Article> e = table[index];
        if (e != null && !e.isDeleted) return e.value;
        return null;
    }

    public Article put(Integer key, Article value) {
        if (key == null) throw new IllegalArgumentException("Key must not be null.");
        if ((double) (size + 1) / table.length > loadFactor) {
            resize(nextPrime(table.length * 2));
        }

        int index = findSlotIndex(key, true); // true -> find or insert position
        if (index == -1) throw new IllegalStateException("Failed to find slot for insertion.");

        Entry<Integer,Article> slot = table[index];

        if (slot == null || slot.isDeleted) {
            table[index] = new Entry<>(key, value);
            size++;
            return null;
        } else {
            // update existing
            Article old = slot.value;
            slot.value = value;
            return old;
        }
    }

    public Article remove(Integer key) {
        int index = findSlotIndex(key, false);
        if (index == -1)
            return null;

        Entry<Integer,Article> e = table[index];

        if (e == null || e.isDeleted)
            return null;

        Article old = e.value;
        e.isDeleted = true;
        size--;
        return old;
    }

    private int findSlotIndex(Integer key, boolean forInsert) {
        long hash = computeHash(key);
        int N = table.length;
        int hash1 = (int) (Math.abs(hash) % N);

        if (collisionType == CollisionType.LINEAR_PROBING) {
            int firstDeletedIndex = -1;
            for (int j = 0; j < N; j++) {
                int idx = (hash1 + j) % N;
                Entry<Integer,Article> e = table[idx];
                if (e == null) {
                    // empty slot
                    if (forInsert) {
                        return (firstDeletedIndex != -1) ? firstDeletedIndex : idx;
                    } else {
                        return -1;
                    }
                } else if (e.isDeleted) {
                    if (forInsert && firstDeletedIndex == -1) firstDeletedIndex = idx;
                } else if (Objects.equals(e.key, key)) {
                    return idx;
                } else {
                    if (forInsert)
                        collisionCount++;
                }
            }
            return -1; // table full or not found
        }

        else if (collisionType == CollisionType.DOUBLE_HASHING) { // DOUBLE_HASHING
            // select q < N, q prime

            int q = prevPrime(N - 1);

            int h2 = (int) (q - Math.abs(hash % q));

            if (h2 == 0) h2 = 1; // ensure non-zero

            int firstDeletedIndex = -1;

            for (int j = 0; j < N; j++) {
                int idx = (hash1 + j * h2) % N;
                Entry<Integer,Article> e = table[idx];
                if (e == null) {
                    if (forInsert) {
                        return (firstDeletedIndex != -1) ? firstDeletedIndex : idx;
                    } else {
                        return -1;
                    }
                } else if (e.isDeleted) {
                    if (forInsert && firstDeletedIndex == -1) firstDeletedIndex = idx;
                } else if (Objects.equals(e.key, key)) {
                    return idx;
                } else {
                    if (forInsert)
                        collisionCount++;
                }
            }
            return -1;
        }
        return -9999;

    }

    private void resize(int newCapacity) {
        int newCapPrime = nextPrime(newCapacity);
        Entry<Integer,Article>[] oldTable = table;

        table = (Entry<Integer,Article>[]) new Entry[newCapPrime];

        int oldSize = size;
        size = 0;
        currentCapacityPrime = newCapPrime;

        // rehash active entries
        for (Entry<Integer,Article> e : oldTable) {
            if (e != null && !e.isDeleted) {
                // put without triggering another resize check to avoid infinite recursion
                rehashPut(e.key, e.value);
            }
        }
    }

    private void rehashPut(Integer key, Article value) {
        long h = computeHash(key);
        int N = table.length;
        int h1 = (int) (Math.abs(h) % N);

        if (collisionType == CollisionType.LINEAR_PROBING) {
            for (int j = 0; j < N; j++) {
                int idx = (h1 + j) % N;
                Entry<Integer,Article> e = table[idx];
                if (e == null || e.isDeleted) {
                    table[idx] = new Entry<>(key, value);
                    size++;
                    return;
                }
            }
        } else {
            int q = prevPrime(N - 1);
            int h2 = (int) (q - Math.abs(h % q));
            if (h2 == 0) h2 = 1;
            for (int j = 0; j < N; j++) {
                int idx = (h1 + j * h2) % N;
                Entry<Integer,Article> e = table[idx];
                if (e == null || e.isDeleted) {
                    table[idx] = new Entry<>(key, value);
                    size++;
                    return;
                }
            }
        }

        throw new IllegalStateException("Rehash failed: table appears to be full.");
    }

    private long computeHash(Integer key) {
        String s = key.toString().toLowerCase();
        if (hashFunctionType == HashFunctionType.SSF) {
            long sum = 0;
            for (int i = 0; i < s.length(); i++) {
                sum += (int) s.charAt(i);
            }
            return sum;
        }
        else if (hashFunctionType == HashFunctionType.PAF) {
            long h = 0;
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                int val;
                if (ch >= 'a' && ch <= 'z')
                    val = ch - 'a' + 1;
                else
                    val = (int) ch; // use raw char value for non-letter

                h = h * PAF_Z + val; // may overflow long; acceptable per assignment
            }
            return h;
        }
        return 0000001;
    }

    /* -----------PRIME CHECKERS------------- */

    private static boolean isPrime(int n) {
        if (n <= 1)
            return false;
        if (n <= 3)
            return true;
        if (n % 2 == 0)
            return false;

        int r = (int) Math.sqrt(n);

        for (int i = 3; i <= r; i += 2) {
            if (n % i == 0)
                return false;
        }
        return true;
    }

    private static int nextPrime(int n) {
        if (n <= 2) return 2;
        int candidate = (n % 2 == 0) ? n + 1 : n;

        while (!isPrime(candidate)){
            candidate += 2;
        }

        return candidate;
    }

    private static int prevPrime(int n) {
        if (n <= 2) return 2;

        int candidate = (n % 2 == 0) ? n - 1 : n;

        while (candidate >= 2 && !isPrime(candidate)) {
            candidate -= 2;
        }

        if (candidate < 2)
            return 2;

        return candidate;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HashMapCustom(size=").append(size).append(", capacity=").append(table.length)
                .append(", collisions=").append(collisionCount).append(")\n");
        for (int i = 0; i < table.length; i++) {
            Entry<Integer,Article> e = table[i];
            sb.append(i).append(": ");
            if (e == null) sb.append("null\n");
            else sb.append(e.key).append("=").append(e.value).append(e.isDeleted ? " (deleted)\n":"\n");
        }
        return sb.toString();
    }
}
