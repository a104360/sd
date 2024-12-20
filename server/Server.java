package server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import client.Client;
import client.ClientList;
import connection.Data;
import connection.FramedConnection;

class PasswordManager {
    private HashMap<String, ArrayList<Client>> clients = new HashMap<>();
    private ReentrantLock lock = new ReentrantLock();

    //Função pra ver se as credenciais existem


    //Funcao pra mudar a password 


    //Funcao pra registar credenciais novas

    public void newUser(Client c){
        lock.lock();
        try{
            if(clients.get(c.getPassword()) == null) {
                ArrayList<Client> clientList = new ArrayList<>();
                clientList.add(c);
                clients.put(c.getPassword(), clientList);
            } else {
                ArrayList<Client> clientList = clients.get(c.getPassword());
                clients.get(c.getPassword()).add(c);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean confirmUser(Client c){
        lock.lock();
        try{
            if(clients.get(c.getPassword()) == null) {
                return false;
            } else {
                ArrayList<Client> clientList = clients.get(c.getPassword());
                for(Client next : clientList) {
                    if(c.equals(next)) return true;
                } 
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    public int updateUser(String password, Client c){
        lock.lock();
        try{
            int canUpdate = 0;
            if(clients.get(password) == null) {
                return 1; // a pass nem esta errada 
            } else {
                ArrayList<Client> clientList = clients.get(password);
                for(Client next : clientList) {
                    if(c.getName().equals(c.getName())) {
                        canUpdate = 1;
                        break;
                    }
                } 
                if(canUpdate == 0) return 2; //a pass existe mas o nome não, logo o homem e cego e não sabe o nome, e cego e errou a pass, ou os 2
                newUser(c);
                return 0; //mudou bem a pass
            }
        } finally {
            lock.unlock();
        }
    }

    /*passar do map de listas para 1 clientLista*/
    public ClientList getClients() {
        ClientList clientList = new ClientList();

        lock.lock();
        try{
            for(ArrayList<Client> cList : clients.values()){
                for(Client c : cList) clientList.add(c);
            }

            return clientList;
        } finally {
            lock.unlock();
        }
    }
}

class ServerWorker implements Runnable {
    private Socket socket;
    private PasswordManager manager;
    private int maxSessions;
    private int activeSessions;
    private FramedConnection c;
    private ReentrantLock lock = new ReentrantLock();
    private Condition canConnect = lock.newCondition();
    private Data store;

    public ServerWorker(Socket socket, PasswordManager manager, int maxSessions, FramedConnection con, int activeSessions, Data dataStore) {
        this.socket = socket;
        this.manager = manager;
        this.maxSessions = maxSessions;
        this.c = con;
        this.activeSessions = activeSessions;
        this.store = dataStore;
    }

    @Override
    /*mudificar para o que se quer */
    public void run() {
        try{
            // Controle de número de sessões com ReentrantLock
            lock.lock();
            try {
                while (activeSessions >= maxSessions) {
                    canConnect.await();  // Aguarda até que haja espaço para mais conexões
                }
                activeSessions++; // Incrementa o contador de sessões ativas
                System.out.println("Client connected. Active sessions: " + activeSessions);
            } finally {
                lock.unlock();
            }


            String username = new String(c.receive());
            String password = new String(c.receive());
            System.out.println(username + " " +password);
            
            Client newUser = new Client(username,password);
            // adicionar user novo
            
            String welcome = "Welcome! You are connected to the server.";
            //System.out.println(welcome.getBytes().length);
            c.send(welcome.getBytes());

            boolean clientExited = false;
            while (!clientExited) {
                
            }

            /* 
            boolean open = true;
            while(open){
                Client c = Client.deserialize(in);
                if(c == null){
                    open = false;
                }else{
                    manager.update(c);
                }
            }
            */

            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
        }catch (IOException | InterruptedException e){
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            // Após o cliente selecionar "exit", decrementa o contador de sessões ativas
            lock.lock();
            try {
                activeSessions--; // Decrementa o contador de sessões ativas
                System.out.println("Client disconnected. Active sessions: " + activeSessions);
                canConnect.signalAll();  // Notifica outras threads esperando para se conectar
            } finally {
                lock.unlock();
            }
        }
    }
}



public class Server {

    public static void main (String[] args) throws IOException {
        int port = 12345;
        int maxSessions = 3;
        int activeSessions = 0;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
        PasswordManager manager = new PasswordManager();
        Data DataStore = new Data();

        // example pre-population
        manager.newUser(new Client("John", "john@mail.com"));
        manager.newUser(new Client("Alice", "CompanyInc."));
        manager.newUser(new Client("Bob", "bob.work@mail.com"));

        while (true) {
            Socket socket = serverSocket.accept();
            FramedConnection c = new FramedConnection(socket);
            Thread worker = new Thread(new ServerWorker(socket, manager,maxSessions,c,activeSessions,DataStore));
            worker.start();
        }
    }

}
