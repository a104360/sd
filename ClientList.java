import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

class ClientList extends ArrayList<Client> {
    private ReentrantLock lock = new ReentrantLock();

    public void serialize(DataOutputStream out) throws IOException { 
        lock.lock();
        
        try {
            out.writeInt(this.size());
            
            for (Client client : this) {
                client.serialize(out);
                //flush fora para mais controlo
            }   
        } finally {
            lock.unlock();
        }
    }

    public static ClientList deserialize(DataInputStream in) throws IOException { 
        ClientList clientList = new ClientList();
        
        int size = in.readInt();
        
        for (int i = 0; i < size; i++) {
            Client client = Client.deserialize(in);
            clientList.add(client);
        }
        
        return clientList;
    }

}
