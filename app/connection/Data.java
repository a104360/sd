package connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.*;
import java.util.List;


public class Data {
    private Map<String, byte[]> store;// = new HashMap<>();
    private ReentrantLock lock = new ReentrantLock(); 
    private Condition waitWhen = lock.newCondition();

    /**
     * Cria instancia Data
     */
    public Data(){
        this.store = new HashMap<>();
    }

    /**
     * Operação de escrita, se a chave não existir, é criada uma nova entrada no servidor, com o par chave-
     * valor enviado. Caso contrário, a entrada deverá ser atualizada com o novo valor.
     * @param key - chave
     * @param value - informação que se quer guardar
     */
    public void put(String key, byte[] value) {
        lock.lock();
        try {
            store.put(key, value);
            this.waitWhen.signalAll();//acordar todos as threads em getWhen 
        } finally {
            lock.unlock();
        }
    }

    /**
     * Operação de leitura, para uma chave key, devolver o respetivo valor, ou null caso a chave não exista.
     * @param key - chave
     * @return - devolve o valor, null caso não exista
     */
    public byte[] get(String key) {
        lock.lock();
        try {
            return store.get(key);
        } finally {
            lock.unlock();
        }
    }

    public void multiPut(Map<String,byte[]> entries){
        this.lock.lock();
        try{
            for (Entry<String,byte[]> value : entries.entrySet()) {
                this.store.put(value.getKey(), value.getValue());
            }
            this.waitWhen.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    public Map<String,byte[]> multiGet(List<String> keys){
        this.lock.lock();
        try{
            Map<String,byte[]> gets = new HashMap<>();
            for (String key : keys) {
                byte[] value = this.store.get(key);    
                gets.put(key,value);
            }
            return gets;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Leitura condicional, devolve o valor da chave key quando o valor da chave
     * keyCond for igual a valueCond, ficando a operação bloqueada até tal acontecer
     * @param key - chave 
     * @param keyCond - chave de procura
     * @param valueCond - value de comparacao
     * @return - value correspondente a key
     */
    public byte[] getWhen(String key, String keyCond, byte[] valueCond) throws InterruptedException {
        this.lock.lock();
        try{
            while(new String(this.store.get(keyCond)).compareTo(new String(valueCond)) != 0){
                //System.out.println("CONDITIONAL KEY : " + new String(this.store.get(keyCond)));
                //System.out.println("CONDITIONAL VALUE : " + new String(valueCond));
                this.waitWhen.await();
            }
            return this.store.get(key);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public String toString(){
        String text = new String();
        for(Map.Entry<String,byte[]> key : this.store.entrySet()){
            System.out.println("KEY : " + key.getKey() + "\nVALUE : " + new String(key.getValue()) + "|\n");
        }
        return text;
    }
}
