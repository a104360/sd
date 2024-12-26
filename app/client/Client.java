package client;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import connection.FramedConnection;

public class Client implements ClientLibrary{
    private String name;
    private String password;
    private FramedConnection c;

    /**
     * Cria um novo cliente
     * @param name - nome do user
     * @param password - pass do user
     */
    public Client(String name, String password,FramedConnection c) throws IOException {
        this.name = name;
        this.password = password;
        this.c = null;
    }

    /**
     * Construtor de Client apartir de uma Framed Connection
     * @param c
     */
    public Client(FramedConnection c){
        this.name = null;
        this.password = null;
        this.c = c;
    }

    /**
     * devolve o nome do user
     * @return
     */
    public String getName() { 
        if(this.name != null) return name; 
        return "\0";
    }

    /**
     * devolve a pass do user
     * @return
     */
    public String getPassword() { 
        if(this.password != null)return password; 
        return "\0";
    }

    public void setName(String name){
        this.name = name;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public boolean hasLeft(){
        return this.name == null || this.password == null ? false : true;
    }

    /**
     * Writes the bytes from a client credentials to the DataOutputStream passed as an argument
     * @param out - DataOutputStream
     * @throws IOException
     */
    public void serialize(DataOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(password);
        //flush fora para mais controlo
    }

    /**
     * Reads the bytes from a client credentials from the DataInputStream passed as an argument
     * @param in - DataInputStream
     * @return - Return dos bytes lidos
     * @throws IOException
     */
    public static Client deserialize(DataInputStream in) throws IOException { 
        String name;
        name = in.readUTF();
        
        String password;
        password = in.readUTF();

        return new Client(name, password,null);
    }

    private boolean register(BufferedReader in) throws IOException{
        this.c.send(FramedConnection.REGISTER.getBytes());
        System.out.print("Enter Username: ");
        String username = in.readLine();
        System.out.print("Enter Password: ");
        String password = in.readLine();
        this.c.send(username.getBytes());
        this.c.send(password.getBytes());
        String reply = new String(this.c.receive());
        System.out.println(reply);
        if(reply.compareTo("ALREADY EXISTS") == 0)return false;
        this.setName(username);
        this.setPassword(password);
        return true;    
    }

    public boolean login(BufferedReader in) throws IOException{
        this.c.send(FramedConnection.LOGIN.getBytes());
        System.out.print("Enter Username: ");
        String username = in.readLine();
        System.out.print("Enter Password: ");
        String password = in.readLine();
        this.c.send(username.getBytes());
        this.c.send(password.getBytes());
        String replyLogin = new String(this.c.receive());
        if(replyLogin.compareTo(FramedConnection.SUCCESS) == 0){
            //cli = new Client(username, password);
            this.setName(username);
            this.setPassword(password);
            System.out.println("Logged In");
            return true;
        }
        System.out.println(replyLogin);
        return false;
    }

    public void changePassword(BufferedReader in) throws IOException{
        this.c.send(FramedConnection.CHANGEPASSWORD.getBytes());
        String changeReply = new String(this.c.receive());
        System.out.println(changeReply);
        if(changeReply.compareTo("LOGIN NECESSARY") == 0){
            return;
        }
        boolean updatePassword = true;
        int tries = 0;
        while (updatePassword) {

            if(tries > 2){
                System.out.print("Do you wich to exit? s/y for yes, any other key for no: ");
                String updateChoice = in.readLine();
                if(updateChoice.toLowerCase().equals("s") || updateChoice.toLowerCase().equals("y")){
                    this.c.send("exit".getBytes());  
                    return;
                }
            }

            this.c.send("try".getBytes());  
            // Solicita o username e a password
            System.out.print("Username: ");
            String username = in.readLine();
            System.out.print("Password: ");
            String password = in.readLine();
            System.out.print("New password: ");
            String nPassword = in.readLine();

            // Envia os dados para o servidor
            this.c.send(username.getBytes());
            this.c.send(password.getBytes());
            this.c.send(nPassword.getBytes());

            // Recebe a resposta do servidor
            byte[] reply = this.c.receive();
            String typeOfReply = new String(reply);

            // Verifica o tipo de resposta
            if (typeOfReply.equals("1")) {
                System.out.println("Incorrect password. Please try again.");
            } else if (typeOfReply.equals("2")) {
                System.out.println("Incorrect password or username. Please try again.");
            } else if (typeOfReply.equals("0")) {
                System.out.println("Changed password successfully to " + nPassword);
                this.name = username;
                this.password = nPassword;
                return; // Sai do loop quando o login for bem-sucedido
            } else {
                System.out.println("Unexpected reply from server: " + typeOfReply);
            }
            tries++;
        }
    }

    public static Client authenticate(BufferedReader in) throws IOException{
        Client cli = new Client(new FramedConnection());
        boolean auth = false;
        boolean valid = true;
            while (valid) {
                System.out.println("\n--- Autentification Menu ---");
                System.out.println("1. Register");
                System.out.println("2. Log in");
                System.out.println("3. Change password"); 
                if(auth == false) System.out.println("4. Exit");
                else System.out.println("4. Main Menu");
                System.out.print("Choose an option: ");
                String passChoice = in.readLine();
                int passOption = Integer.parseInt(passChoice);

                switch (passOption) {
                    case 1: // register
                        auth = cli.register(in);
                        break;

                    case 2: // Login 
                        auth = cli.login(in);
                        break;
                    
                    case 3: // Change Password
                        cli.changePassword(in);
                        break;

                    case 4:
                        System.out.println("Exiting...");
                        valid = false;
                        if(!auth){
                            cli.c.close();
                            in.close();
                            return null;
                        } 
                        cli.c.send(FramedConnection.NEXTSTEP.getBytes());
                        break;

                    default:
                        System.out.println("Invalid choice.");
                }
            }
        return cli;
    }
    
    public void put(String key, byte[] value){
        try{
            this.c.send(FramedConnection.PUT.getBytes());
            this.c.send(key.getBytes());
            this.c.send(value);
            System.out.println(new String(this.c.receive()));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    public byte[] get(String key){
        try{
            this.c.send(FramedConnection.GET.getBytes());
            this.c.send(key.getBytes());
            return this.c.receive();
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public void multiPut(Map<String,byte[]> pairs){
        try{
            for(Map.Entry<String,byte[]> entry : pairs.entrySet()){
                this.c.send(entry.getKey().getBytes());
                this.c.send(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String,byte[]> multiGet(Set<String> keys){
        try{
            Map<String,byte[]> mapa = new HashMap<>();
            for(String s : keys){
                this.c.send(s.getBytes());
            }
            
            for(String s : keys){
                mapa.put(s,this.c.receive());
            }
            return mapa;
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }   

    public byte[] getWhen(String key,String keyCond,byte[] valueCond){
        try{
            this.c.send(key.getBytes());
            this.c.send(keyCond.getBytes());
            this.c.send(valueCond);

            return this.c.receive();
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Name : ");
        builder.append(this.name).append("\n");
        builder.append("Password : ");
        builder.append(this.password);
        builder.append("|\n");
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            System.out.println("apenas comparou o pointer");
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Client other = (Client) obj;
        return this.name.compareTo(other.name) == 0;
        //return Objects.equals(name, other.getName()) && Objects.equals(password, other.getPassword());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, password);
    }

    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        Client cli = Client.authenticate(in);
        
        try{
            cli.hasLeft();
        } catch (NullPointerException e){
            return;
        }
        
        System.out.println(new String(cli.c.receive()));
        boolean running = true;
            while (running) {
                System.out.println("\n--- Main Menu ---");
                System.out.println("1. Put (store a value)");
                System.out.println("2. Get (retrieve a value)");
                System.out.println("3. MultiPut (store multiple values)");
                System.out.println("4. MultiGet (retrieve multiple values)");
                System.out.println("5. GetWhen (retrieve one value when ...)");
                System.out.println("6. Exit");
                System.out.print("Choose an option: ");
                String choice = in.readLine();
                int option = Integer.parseInt(choice);

                switch (option) {
                    case 1: // PUT
                        System.out.print("Enter key: ");
                        String key = in.readLine();
                        System.out.print("Enter value: ");
                        String value = in.readLine();
                        cli.put(key,value.getBytes());
                        break;

                    case 2: // GET 
                        System.out.print("Enter key: ");
                        key = in.readLine();
                        byte[] response = cli.get(key);
                        System.out.println(new String(response));
                        break;
                    
                    case 3: // MULTIPUT
                        cli.c.send(FramedConnection.MULTIPUT.getBytes());
                        int multiPutKey = 0; // Variável para armazenar o número inteiro
                        boolean validMultiPutInput = false;

                        while (!validMultiPutInput) {
                            System.out.println("How many values do you want: ");

                            try {
                                String line = in.readLine();
                                multiPutKey = Integer.parseInt(line); // Tenta converter para inteiro
                                validMultiPutInput = true; // Se for bem-sucedido, sai do loop
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input. Please enter a valid number.");
                            } catch (IOException e) {
                                System.out.println("Error reading input: " + e.getMessage());
                            }
                        }
                        System.out.println("You entered the integer: " + multiPutKey);
                        cli.c.send(String.valueOf(multiPutKey).getBytes()); //strasforma a int em string e envia
                        Map <String,byte[]> toSend = new LinkedHashMap<>();
                        for (int i = 1; i <= multiPutKey; i++) {
                            System.out.print("Enter key " + i + ": ");
                            String putKey = in.readLine(); // Lê a chave do usuário
                            System.out.print("Enter value for key " + i + ": ");
                            String putValue = in.readLine(); // Lê o valor do usuário
                            toSend.put(putKey,putValue.getBytes());
                            //cli.c.send(putKey.getBytes());
                            //cli.c.send(putValue.getBytes());
                        }
                        cli.multiPut(toSend);
                        System.out.println(new String(cli.c.receive()));
                        break;

                    case 4: // MULTIGET
                        cli.c.send(FramedConnection.MULTIGET.getBytes());
                        int multiGetKey = 0; // Variável para armazenar o número inteiro
                        boolean validMultiGetInput = false;
                

                        // Bloco onde se indica o numero de entradas a obter
                        while (!validMultiGetInput) {
                            System.out.println("How many values do you want: ");

                            try {
                                String line = in.readLine();
                                multiGetKey = Integer.parseInt(line); // Tenta converter para inteiro
                                validMultiGetInput = true; // Se for bem-sucedido, sai do loop
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input. Please enter a number.");
                            } catch (IOException e) {
                                System.out.println("Error reading input: " + e.getMessage());
                            }
                        }
                        System.out.println("You entered the integer: " + multiGetKey);
                        cli.c.send(String.valueOf(multiGetKey).getBytes()); //trasforma a int em string e envia

                        // Obtencao dos pares
                        Set<String> keys = new LinkedHashSet<>();
                        for (int i = 1; i <= multiGetKey; i++) {
                            System.out.print("Enter key " + i + ": ");
                            String getKey = in.readLine(); // Lê a chave do usuário
                            //cli.c.send(getKey.getBytes());
                            keys.add(getKey);
                        }

                        Map<String,byte[]> getPairs = cli.multiGet(keys);
                        
                        /*// Receber os byte array
                        for(String temp : getPairs.keySet()){
                            byte[] valueInByte = cli.c.receive();
                            getPairs.put(temp, valueInByte);
                        }*/

                        for (Map.Entry<String, byte[]> entry : getPairs.entrySet()) {
                            String tempKey = entry.getKey();       // Obtém a chave
                            byte[] tempResult = entry.getValue(); // Obtém o valor (byte[])
                            // Verifica e processa o valor
                            if (tempResult != null) {
                                System.out.println("Key: " + tempKey + ", Value: " + new String(tempResult));
                            } else {
                                System.out.println("Key: " + tempKey + " has no associated value.");
                            }
                        }
                        break;

                    case 5: // GET WHEN
                        cli.c.send(FramedConnection.GETWHEN.getBytes());
                        System.out.println("A função devolve o valor da primeira chave que meter como input,\n" +
                                           "quando o valor da segunda chave que meter indicar for igual ao valor que meter,\n" +
                                           "so valtando a ser possivel fazer outras operaccões até tal acontecer");
                        
                        System.out.print("Enter key: ");
                        String firstKey = in.readLine();
                        System.out.print("Enter conditional key: ");
                        String secondKey = in.readLine();
                        System.out.print("Enter conditional value: ");
                        String compareValue = in.readLine();
                        
                        String valueWhen = new String(cli.getWhen(firstKey, secondKey, compareValue.getBytes()));
                        
                        System.out.println(valueWhen);
                        break;

                    case 6:
                        System.out.println("Exiting...");
                        running = false;
                        cli.c.close();
                        break;

                    default:
                        System.out.println("Invalid choice.");
                }
            }    
    }

}