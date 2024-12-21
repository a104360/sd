package server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

import client.Client;
import client.ClientList;
import connection.Data;
import connection.FramedConnection;

class PasswordManager {
    /**
     * Estrutura para guardar o nome do Cliente a sua password
     * @apiNote Varios clientes podem ter a mesma password
     */
    private Map<String, ArrayList<Client>> clients = new HashMap<>();
    /**
     * Lock para bloquear o PassowordManager
     */
    private ReentrantLock lock = new ReentrantLock();

    //Função pra ver se as credenciais existem


    //Funcao pra mudar a password 


    //Funcao pra registar credenciais novas
    /**
     * Inserir novos utilizadores no mapa de clientes
     * @param c - Cliente a inserir
     */
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

    /**
     * Confirmar se o utilizador está registado no sistema
     * @param c - cliente a verificar
     * @return 
     */
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

    /**
     * Atualizar a password do cliente, se a password antiga estiver correta
     * @param password - password atual do cliente 
     * @param c - Instancia do cliente a atualizar
     * @return
     */
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
    private Server server;
    private PasswordManager manager;
    private int maxSessions;
    private int activeSessions;
    private FramedConnection c;
    private ReentrantLock lock = new ReentrantLock();
    private Condition canConnect = lock.newCondition();
    private Data store;

    public ServerWorker(Server server,Socket socket, PasswordManager manager, int maxSessions, FramedConnection con, int activeSessions, Data dataStore) {
        this.server = server;
        this.socket = socket;
        this.manager = manager;
        this.maxSessions = maxSessions;
        this.c = con;
        this.activeSessions = activeSessions;
        this.store = dataStore;
    }

    @Override
    /*modificar para o que se quer */
    public void run() {
        try{
            /*// Controle de número de sessões com ReentrantLock
            lock.lock();
            try {
                while (server.activeSessions >= maxSessions) {
                    canConnect.await();  // Aguarda até que haja espaço para mais conexões
                }
                server.activeSessions++; // Incrementa o contador de sessões ativas
                System.out.println("Client connected. Active sessions: " + server.activeSessions);
            } finally {
                lock.unlock();
            }*/


            String username = new String(c.receive());
            String password = new String(c.receive());
            System.out.println(username + " " +password);
            
            Client newUser = new Client(username,password);
            // adicionar user novo
            
            String welcome = "Welcome! You are connected to the server.";
            //System.out.println(welcome.getBytes().length);
            c.send(welcome.getBytes());

            //boolean clientExited = false;
            while (socket.isConnected()) {
                // Logica para tratar pedidos
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
        }catch (IOException e){//| InterruptedException e){
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            // Após o cliente selecionar "exit", decrementa o contador de sessões ativas
            lock.lock();
            try {
                server.activeSessions--; // Decrementa o contador de sessões ativas
                System.out.println("Client disconnected. Active sessions: " + server.activeSessions);
                server.serverCondition.signalAll();  // Notifica outras threads esperando para se conectar
            } finally {
                lock.unlock();
            }
        }
    }
}



public class Server {
    Lock serverLock = new ReentrantLock();
    Condition serverCondition = serverLock.newCondition();
    int port;
    int maxSessions;
    int activeSessions;
    ServerSocket serverSocket;
    PasswordManager manager;
    Data dataStore;
    
    public Server() throws IOException{
        this.port = 12345;
        this.maxSessions = 3;
        this.activeSessions = 0;
        this.serverSocket = new ServerSocket(port);
        this.manager = new PasswordManager();
        this.dataStore = new Data();
    }
    
    public void getConnections(){
        System.out.println("Active Connections: " + this.activeSessions + "\nMaxConnections : " + this.maxSessions);
    }

    public static void main (String[] args) throws IOException, InterruptedException{
        Server s = new Server();

        // example pre-population
        s.manager.newUser(new Client("John", "john@mail.com"));
        s.manager.newUser(new Client("Alice", "CompanyInc."));
        s.manager.newUser(new Client("Bob", "bob.work@mail.com"));

        s.getConnections();

        while (true) {
            Socket socket = s.serverSocket.accept();
            s.serverLock.lock();
            try{
                if(s.activeSessions < s.maxSessions){
                    s.activeSessions++;
                } else {
                    while(s.activeSessions >= s.maxSessions){
                        s.serverCondition.await();
                    }
                }
            } finally {
                s.serverLock.unlock();
            }
            s.getConnections();
            FramedConnection c = new FramedConnection(socket);
            Thread worker = new Thread(new ServerWorker(s,socket, s.manager,s.maxSessions,c,s.activeSessions,s.dataStore));
            worker.start();
        }
    }

}
