package connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.*;


public class Data {
    private final Map<String, byte[]> store = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock(); 

    public void put(String key, byte[] value) {
        lock.lock();
        try {
            store.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    public byte[] get(String key) {
        lock.lock();
        try {
            return store.get(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString(){
        String text = new String();
        for(Map.Entry<String,byte[]> key : this.store.entrySet()){
            System.out.println("KEY : " + key.getKey() + "\nVALUE : " + key.getValue() + "|\n");
        }
        return text;
    }
}
