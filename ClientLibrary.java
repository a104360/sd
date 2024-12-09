import java.util.Map;
import java.util.Set;

public interface ClientLibrary {
    void put(String key,byte[] value);
    byte[] get(String key);

    void multiPut(Map<String,byte[]> pairs);
    Map<String,byte[]> multiGet(Set<String> keys);
}
