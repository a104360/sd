package server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.lang.ProcessBuilder;

import client.Client;
import client.ClientList;
import connection.Data;
import connection.FramedConnection;

class PasswordManager {
    /**
     * Estrutura para guardar o nome do Cliente a sua password
     * @apiNote Varios clientes podem ter a mesma password
     */
    private Map<String, List<Client>> clients = new HashMap<>();
    /**
     * Lock para bloquear o PassowordManager
     */
    private ReentrantLock lock = new ReentrantLock();


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
                List<Client> clientList = clients.get(c.getPassword());
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
                List<Client> clientList = clients.get(c.getPassword());
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
     * @param c - Instancia do cliente a atualizar com a nova palavra pass
     * @return 
     * 0 - sucesso \\
     * 1 - password incorreta \\ 
     * 2 - password ou username incorreto
     */
    public int updateUser(String password, Client c){
        lock.lock();
        try{
            if(clients.get(password) == null) {
                return 1;
            } else {
                List<Client> clientList = clients.get(password);
                for(Client next : clientList) {
                    if(next.getName().equals(c.getName())) {
                        clientList.remove(next);
                        newUser(c);
                        return 0;
                    }
                } 
                return 2; 
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Obter um objeto ClientList do Map de Password
     */
    public ClientList getClients() {
        ClientList clientList = new ClientList();

        lock.lock();
        try{
            for(List<Client> cList : clients.values()){
                for(Client c : cList) clientList.add(c);
            }

            return clientList;
        } finally {
            lock.unlock();
        }
    }
    @Override
    public String toString(){
        StringBuilder text = new StringBuilder();
        for(String password : this.clients.keySet()){
            for(Client c : this.clients.get(password)){
                text.append(c.toString());
            }
        }
        return text.toString();
    }
}

class ServerWorker implements Runnable {
    private Server server;
    private PasswordManager manager;
    private FramedConnection c;
    private Data store;

    public ServerWorker(Server server, PasswordManager manager, int maxSessions, FramedConnection con, int activeSessions, Data dataStore) {
        this.server = server;
        this.manager = manager;
        this.c = con;
        this.store = dataStore;
    }

    private Client getCredentials() throws IOException{
        String username = new String(this.c.receive());
        String password = new String(this.c.receive());
        System.out.println(username + " " +password);
        
        return new Client(username,password);
    }

    @Override
    /**
     * Funcao que implementa o comportamento para atender clientes
     */
    public void run(){
        try{
            // Receber o request
            String request = new String(c.receive());
            Client validUser = null; // Indica se o utilizador e valido
            boolean nextStep = false; // Indica se podemos receber os pedidos de operaçoes
            // Iterar este ciclo enquanto 
            //o utilizador for invalido ou nao podermos passar para a rececao de operações 
            while (validUser == null || !nextStep) {
                //System.out.println("-----------------");
                this.server.debug();
                //System.out.println(validUser);
                //System.out.println(nextStep);
                switch (request) {
                    case FramedConnection.REGISTER:
                        System.out.println("REG");
                        Client c = getCredentials();
                        this.manager.newUser(c);
                        this.c.send("LOGGED IN".getBytes());
                        validUser = c;
                        this.server.debug();
                        request = new String(this.c.receive());
                        System.out.println("RECEBEU PROXIMO");
                        break;
                    case FramedConnection.LOGIN:
                        System.out.println("LOG");
                        Client aux = getCredentials();
                        if(!this.manager.confirmUser(aux)){
                            this.c.send("Credenciais invalidas".getBytes());
                            request = new String(this.c.receive());
                            break;
                        }
                        validUser = aux;
                        this.server.debug();
                        this.c.send(FramedConnection.SUCCESS.getBytes());
                        request = new String(this.c.receive());
                        break;
                    case FramedConnection.CHANGEPASSWORD:
                        System.out.println("CGP");
                        if(validUser == null){
                            this.c.send("LOGIN NECESSARY".getBytes());
                            request = new String(this.c.receive());
                            break;
                        }
                        this.c.send("CONTINUE".getBytes());
                        int reply = 1;
                        for(int i = 0; i < 3;i++){
                            String user = new String(this.c.receive());
                            String old = new String(this.c.receive());
                            String nPassword = new String(this.c.receive());
                            Client temp = new Client(user, nPassword);
                            reply = this.manager.updateUser(old,temp);
                            this.c.send((Integer.toString(reply)).getBytes());
                            if(reply == 0) break;
                        }
                        this.server.debug();
                        request = new String(this.c.receive());
                        break;
                    case FramedConnection.NEXTSTEP:
                        System.out.println("RECEBEU O NEXTSTEP");
                        nextStep = true;
                        this.server.debug();
                        if(validUser == null){
                            this.c.close();
                            return;
                        }
                        break;
                    default:
                        this.c.send("Request invalido".getBytes());
                        break;
                }          
            }
            System.out.println("SAIU DO CICLO");
            String welcome = "Please select the next operations.";
            c.send(welcome.getBytes());

            
            while (true) {
                // Logica para tratar pedidos
                try{
                    String text = new String(c.receive());
                    switch (text) {
                        case FramedConnection.GET:
                            this.server.debug();
                            System.out.println("RECEIVED " + FramedConnection.GET);
                            String key = new String(this.c.receive());
                            byte[] value = this.store.get(key);
                            if(value != null){
                                this.c.send(value);
                                break;
                            }
                            this.c.send("null".getBytes());
                            break;
                        case FramedConnection.PUT:
                            this.server.debug();
                            System.out.println("RECEIVED " + FramedConnection.PUT);
                            //key = new String(this.c.receive());
                            //value = this.c.receive();
                            break;
                        case FramedConnection.MULTIGET:
                            this.server.debug();
                            System.out.println("RECEIVED " + FramedConnection.MULTIGET);
                            break;
                        case FramedConnection.MULTIPUT:
                            this.server.debug();
                            System.out.println("RECEIVED " + FramedConnection.MULTIPUT);
                            break;
                        case FramedConnection.GETWHEN:
                            this.server.debug();
                            System.out.println("RECEIVED " + FramedConnection.GETWHEN);
                            break;
                        default:
                            System.out.println("ALGO CORREU MAL");
                            /*this.server.serverLock.lock();
                            try{
                                this.server.activeSessions--;
                                this.server.serverCondition.signalAll();
                            } finally {
                                this.server.serverLock.unlock();
                            }
                            this.server.debug();
                            */
                            return;
                    }
                } catch (IOException e){
                    System.err.println(e.getMessage());
                    break;
                    /*this.server.serverLock.lock();
                    try{
                        this.server.activeSessions--;
                        this.server.serverCondition.signalAll();
                    } finally {
                        this.server.serverLock.unlock();
                    }
                    break;*/
                }
                // Login / Registo
                // Ciclo para tratar pedidos
            }
            
            c.close();
        }catch(InterruptedException e){
            System.err.println("Interruption of bigger try : " + e.getMessage());
            /*this.server.serverLock.lock();
            try{
                this.server.activeSessions--;
                this.server.serverCondition.signalAll();
            } finally {
                this.server.serverLock.unlock();
            }*/
        }catch (IOException e){//| InterruptedException e){
            System.err.println("Error handling client: " + e.getMessage());
            /*this.server.serverLock.lock();
            try{
                this.server.activeSessions--;
                this.server.serverCondition.signalAll();
            } finally {
                this.server.serverLock.unlock();
            }*/
        } finally {
            // Após o cliente selecionar "exit", decrementa o contador de sessões ativas
            server.serverLock.lock();
            try {
                server.activeSessions--; // Decrementa o contador de sessões ativas
                System.out.println("Client disconnected. Active sessions: " + server.activeSessions);
                server.serverCondition.signalAll();  // Notifica outras threads esperando para se conectar
                this.server.debug();
            } catch(IOException | InterruptedException e){
                System.err.println(e.getMessage());
            }finally {
                server.serverLock.unlock();
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

    public static void main (String[] args) throws IOException, InterruptedException{
        Server s = new Server();

        // example pre-population
        s.manager.newUser(new Client("John", "john123"));
        s.manager.newUser(new Client("Alice", "CompanyInc."));
        s.manager.newUser(new Client("Bob", "uminho"));

        s.dataStore.put("t", "lorem ipsum".getBytes());
        s.dataStore.put("teste", "LOREM IPSUM".getBytes());

        s.debug();

        while (true) {
            Socket socket = s.serverSocket.accept();
            s.serverLock.lock();
            try{
                while(s.activeSessions >= s.maxSessions){
                    s.serverCondition.await();
                }
                s.activeSessions++;
            } finally {
                s.serverLock.unlock();
            }
            s.debug();
            FramedConnection c = new FramedConnection(socket);
            Thread worker = new Thread(new ServerWorker(s, s.manager,s.maxSessions,c,s.activeSessions,s.dataStore));
            worker.start();
        }
    }

    public void debug() throws IOException,InterruptedException{
        Process processBuilder = new ProcessBuilder("clear").inheritIO().start();
        processBuilder.waitFor();
        System.out.println("----------------------");
        System.out.println("Active sessions : ".toUpperCase() + this.activeSessions);
        System.out.println("Max sessions allowed : ".toUpperCase() + this.maxSessions);
        System.out.println("----------------------");
        System.out.println("MANAGER");
        System.out.println("----------------------");
        System.out.println(this.manager.toString());
        System.out.println("----------------------");
        System.out.println("DATASTORE");
        System.out.println("----------------------");
        System.out.println(this.dataStore.toString());
        System.out.println("----------------------");
    }
}
