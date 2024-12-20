package client;
import java.util.Map;
import java.util.Set;

public interface ClientLibrary {
    public void put(String key,byte[] value);
    public byte[] get(String key);

    public void multiPut(Map<String,byte[]> pairs);
    public Map<String,byte[]> multiGet(Set<String> keys);

    public byte[] getWhen(String key,String keyCond, byte[] valueCond);
}
