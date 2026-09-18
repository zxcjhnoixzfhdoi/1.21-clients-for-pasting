package hack.echo.client.utils.collections;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class Int64Set {
    long bits = 0;

    public int size() {
        return Long.bitCount(bits);
    }

    public int capacity() {
        return 64;
    }

    public boolean add(int key) {
        if (key >= 64) throw new RuntimeException("Key can not exceed 63");

        long temp = bits;
        bits |= (1L << key);
        return temp != bits;
    }

    public boolean remove(int key) {
        if (key >= 64) throw new RuntimeException("Key can not exceed 63");

        long temp = bits;
        long mask = ~(1L << key);
        bits &= mask;
        return temp != bits;
    }

    public boolean contains(int key) {
        if (key >= 64) throw new RuntimeException("Key can not exceed 63");

        return ((bits >> key) & 1) != 0;
    }

    public void clear() {
        bits = 0;
    }

}
