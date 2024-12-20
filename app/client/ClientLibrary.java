package client;
import java.util.Map;
import java.util.Set;

public interface ClientLibrary {

    /**
     * Operação de escrita, se a chave não existir, é criada uma nova entrada no servidor, com o par chave-
     * valor enviado. Caso contrário, a entrada deverá ser atualizada com o novo valor.
     * @param key - chave
     * @param value - informação que se quer guardar
     */
    public void put(String key,byte[] value);

    /**
     * Operação de leitura, para uma chave key, devolver o respetivo valor, ou null caso a chave não exista.
     * @param key - chave
     * @return - devolve o valor, null caso não exista
     */
    public byte[] get(String key);

    /**
     * Operação de escrita composta, todos os pares chave-valor são atualizados / inseridos atomicamente.
     * @param pairs Map com chave Key e tem para cada key o seu value
     */
    public void multiPut(Map<String,byte[]> pairs);

    /**
     * Operação de leitura composta, dado um conjunto de chaves, devolve o conjunto de pares chave-valor respetivo.
     * @param keys - todas as keys para as quais se quer o seu respetivo value
     * @return - Map com um conjunto de pares chave-valor
     */
    public Map<String,byte[]> multiGet(Set<String> keys);

    /**
     * Leitura condicional, devolve o valor da chave key quando o valor da chave
     * keyCond for igual a valueCond, ficando a operação bloqueada até tal acontecer
     * @param key - chave 
     * @param keyCond - chave de procura
     * @param valueCond - value de comparacao
     * @return - value correspondente a key
     */
    public byte[] getWhen(String key,String keyCond, byte[] valueCond);
}
