package server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.lang.ProcessBuilder;

import client.Client;
import connection.Data;
import connection.FramedConnection;
import java.util.Set;

class PasswordManager {
    /**
     * Estrutura para guardar o nome do Cliente a sua password
     * @apiNote Varios clientes podem ter a mesma password
     */
    private Map<String, Set<String>> clients = new HashMap<>();
    /**
     * Lock para bloquear o PassowordManager
     */
    private ReentrantLock lock = new ReentrantLock();


    /**
     * Inserir novos utilizadores no mapa de clientes
     * @param c - Cliente a inserir
     */
    public boolean newUser(Client c){
        // Obter o lock
        lock.lock();
        try{

            //if(this.confirmUser(c) == false) return false;

            // Se nao existir a password
            if(clients.get(c.getPassword()) == null) {
                // Criar uma nova lista de clients
                Set<String> clientList = new HashSet<>();
                // Adicionar o client que queremos inserir no sistema
                clientList.add(c.getName());
                // Adicionar a entrada com a password e uma lista com o client respetivo
                clients.put(c.getPassword(), clientList);
                return true;
            } else {
                if(clients.get(c.getPassword()).contains(c.getName())) return false;
                clients.get(c.getPassword()).add(c.getName());
                return true;
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
            for(Set<String> s : clients.values()){
                if(s.contains(c.getName())) return true;
                /*for(Client b : s){
                    if(c.equals(b)) return true;
                }*/

            }
            return false;
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
                Set<String> clientList = clients.get(password);
                for(String next : clientList) {
                    if(next.equals(c.getName())) {
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

    @Override
    public String toString(){
        try{
            StringBuilder text = new StringBuilder();
            for(String password : this.clients.keySet()){
                for(String c : this.clients.get(password)){
                    text.append(new Client(c,password,null).toString());
                }
            }
            return text.toString();
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
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
        
        return new Client(username,password,null);
    }

    @Override
    /**
     * Funcao que implementa o comportamento para atender clientes
     */
    public void run(){
        try{
            c.send(FramedConnection.SUCCESS.getBytes());
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
                        if(this.manager.confirmUser(c)){
                            this.c.send("ALREADY EXISTS".getBytes());
                            this.server.debug();
                            request = new String(this.c.receive());
                            continue;
                        }
                        this.manager.newUser(c);
                        this.c.send("Registered".getBytes());
                        //else this.c.send("ALREADY EXISTS".getBytes());
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
                        this.c.send("Logged In".getBytes());
                        int reply = 1;
                        String cond = new String(this.c.receive());
                        while(cond.equals("try")){
                            String user = new String(this.c.receive());
                            String old = new String(this.c.receive());
                            String nPassword = new String(this.c.receive());
                            Client temp = new Client(user, nPassword,null);
                            reply = this.manager.updateUser(old,temp);
                            this.c.send((Integer.toString(reply)).getBytes());
                            if(reply == 0) break;
                            cond = new String(this.c.receive());
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

                        case FramedConnection.PUT:
                            this.server.debug();
                            System.out.println("RECEIVED " + FramedConnection.PUT);
                            String key = new String(this.c.receive());
                            byte[] value = this.c.receive();
                            store.put(key, value);
                            //this.c.send("DATA PUT".getBytes());
                            this.server.debug();
                            break;

                        case FramedConnection.GET:
                            this.server.debug();
                            System.out.println("RECEIVED " + FramedConnection.GET);
                            key = new String(this.c.receive());
                            value = this.store.get(key);
                            if(value != null){
                                this.c.send(value);
                                break;
                            }
                            this.c.send("null".getBytes());
                            break;

                        case FramedConnection.MULTIPUT:
                            this.server.debug();
                            System.out.println("RECEIVED " + FramedConnection.MULTIPUT);
                            String multiPutKey = new String(this.c.receive());
                            int size = Integer.parseInt(multiPutKey);


                            Map<String,byte[]> entries = new HashMap<>();
                            for(int i = 0; i < size; i++){
                                key = new String(this.c.receive());
                                value = this.c.receive();
                                entries.put(key, value);
                                //store.put(key, value);
                            }
                            store.multiPut(entries);

                            this.c.send("DATA MULTIPUT".getBytes());
                            this.server.debug();
                            break;

                        case FramedConnection.MULTIGET:
                            this.server.debug();
                            System.out.println("RECEIVED " + FramedConnection.MULTIGET);
                            // Receber o numero de entradas
                            String multiGetKey = new String(this.c.receive());
                            size = Integer.parseInt(multiGetKey);
                            
                            // Receber as keys para procurar
                            List<String> keys = new ArrayList<>();
                            for(int i = 0; i < size; i++){
                                key = new String(this.c.receive());
                                keys.add(key);
                            }

                            // Obtenção do mapa de key e value
                            Map<String,byte[]> values = this.store.multiGet(keys);

                            // Envio dos values para o client
                            for(String k : keys){
                                value = values.get(k);
                                if(value != null) c.send(value);
                                else c.send("null".getBytes());
                            }

                            break;

                        case FramedConnection.GETWHEN:
                            this.server.debug();
                            System.out.println("RECEIVED " + FramedConnection.GETWHEN);

                            String firstKey = new String(this.c.receive());
                            String secondKey = new String(this.c.receive());
                            byte[] compareValue = this.c.receive();

                            byte[] resultWhen = store.getWhen(firstKey, secondKey, compareValue);
                            if(resultWhen != null){
                                this.c.send(resultWhen);
                                break;
                            }
                            this.c.send("null".getBytes());
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
        s.manager.newUser(new Client("John", "john123",null));
        s.manager.newUser(new Client("Alice", "CompanyInc.",null));
        s.manager.newUser(new Client("Bob", "uminho",null));
        s.manager.newUser(new Client("j", "j",null));
        s.manager.newUser(new Client("c", "c",null));

        s.dataStore.put("t", "lorem ipsum".getBytes());
        s.dataStore.put("teste", "LOREM IPSUM".getBytes());
        s.dataStore.put("b", "bernardo".getBytes());
        s.dataStore.put("a", "antonio".getBytes());

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
        //Process processBuilder = new ProcessBuilder("clear").inheritIO().start();
        //processBuilder.waitFor();
        //System.out.println("----------------------");
        //System.out.println("Active sessions : ".toUpperCase() + this.activeSessions);
        //System.out.println("Max sessions allowed : ".toUpperCase() + this.maxSessions);
        //System.out.println("----------------------");
        //System.out.println("MANAGER");
        //System.out.println("----------------------");
        //System.out.println(this.manager.toString());
        //System.out.println("----------------------");
        //System.out.println("DATASTORE");
        //System.out.println("----------------------");
        //System.out.println(this.dataStore.toString());
        //System.out.println("----------------------");
    }
}