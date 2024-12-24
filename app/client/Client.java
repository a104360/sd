package client;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import connection.FramedConnection;

public class Client {
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

    public static Client authenticate(BufferedReader in) throws IOException{
        Client cli = new Client(new FramedConnection());
        boolean auth = false;
        boolean valid = true;
            while (valid) {
                System.out.println("\n--- Autentification Menu ---");
                System.out.println("1. Register");
                System.out.println("2. Log in");
                System.out.println("3. Change password");
                System.out.println("4. Exit");
                System.out.print("Choose an option: ");
                String passChoice = in.readLine();
                int passOption = Integer.parseInt(passChoice);

                switch (passOption) {
                    case 1: // register
                        cli.c.send(FramedConnection.REGISTER.getBytes());
                        System.out.print("Enter key: ");
                        String key = in.readLine();
                        System.out.print("Enter value: ");
                        String value = in.readLine();
                        cli.c.send(key.getBytes());
                        cli.c.send(value.getBytes());
                        System.out.println(new String(cli.c.receive()));
                        //cli = new Client(key, value);
                        cli.setName(key);
                        cli.setPassword(value);
                        auth = true;
                        break;

                    case 2: // Login 
                        cli.c.send(FramedConnection.LOGIN.getBytes());
                        System.out.print("Enter key: ");
                        key = in.readLine();
                        System.out.print("Enter Value: ");
                        value = in.readLine();
                        cli.c.send(key.getBytes());
                        cli.c.send(value.getBytes());
                        String replyLogin = new String(cli.c.receive());
                        System.out.println(replyLogin);
                        if(replyLogin.compareTo(FramedConnection.SUCCESS) == 0){
                            //cli = new Client(key, value);
                            cli.setName(key);
                            cli.setPassword(value);
                            auth = true;
                            break;
                        }
                        /*byte[] result = "null".getBytes();
                        // result = data.get(key);
                        if (result != null) {
                            System.out.println("Value: " + new String(result));
                        }*/
                        break;
                    
                    case 3: // Change Password
                        cli.c.send(FramedConnection.CHANGEPASSWORD.getBytes());
                        String changeReply = new String(cli.c.receive());
                        System.out.println(changeReply);
                        if(changeReply.compareTo("LOGIN NECESSARY") == 0){
                            System.out.println(changeReply);
                            continue;
                        }
                        boolean updatePassword = true;
                        int tries = 0;
                        while (updatePassword) {

                            if(tries > 2){
                                System.out.print("Do you wich to exit? s/y for yes, any other key for non");
                                String updateChoice = in.readLine();
                                if(updateChoice.toLowerCase().equals("s") || updateChoice.toLowerCase().equals("y"));
                                else break;
                            }

                            // Solicita o username e a password
                            System.out.print("Username: ");
                            String username = in.readLine();
                            System.out.print("Password: ");
                            String password = in.readLine();
                            System.out.print("New password : ");
                            String nPassword = in.readLine();
                
                            // Envia os dados para o servidor
                            cli.c.send(username.getBytes());
                            cli.c.send(password.getBytes());
                            cli.c.send(nPassword.getBytes());
                
                            // Recebe a resposta do servidor
                            byte[] reply = cli.c.receive();
                            String typeOfReply = new String(reply);
                
                            // Verifica o tipo de resposta
                            if (typeOfReply.equals("1")) {
                                System.out.println("Incorrect password. Please try again.");
                            } else if (typeOfReply.equals("2")) {
                                System.out.println("Incorrect password or username. Please try again.");
                            } else if (typeOfReply.equals("0")) {
                                System.out.println("Login successful!");
                                cli.name = username;
                                cli.password = nPassword;
                                updatePassword = false; // Sai do loop quando o login for bem-sucedido
                            } else {
                                System.out.println("Unexpected reply from server: " + typeOfReply);
                            }
                            tries++;
                        }

                        if(updatePassword == false) {
                            //System.out.print("New password : ");
                            //String nPassword = in.readLine();
                            //cli.c.send(nPassword.getBytes());
            
                        }
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
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Client other = (Client) obj;
        return this.hashCode() == other.hashCode();
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

        /*if(cli.hasLeft()) {
            if(!cli.c.isClosed()) cli.c.close();
            in.close();
            return;
        }*/
        
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
                    case 1:
                        cli.c.send(FramedConnection.PUT.getBytes());
                        System.out.print("Enter key: ");
                        String key = in.readLine();
                        System.out.print("Enter value: ");
                        String value = in.readLine();
                        cli.c.send(key.getBytes());
                        cli.c.send(value.getBytes());
                        System.out.println(new String(cli.c.receive()));
                        break;

                    case 2:
                        cli.c.send(FramedConnection.GET.getBytes());
                        System.out.print("Enter key: ");
                        key = in.readLine();
                        cli.c.send(key.getBytes());
                        value = new String(cli.c.receive());
                        System.out.println(value);
                        /*byte[] result = "null".getBytes();
                        if (result != null) {
                            System.out.println("Value: " + new String(result));
                        }*/
                        break;
                    
                    case 3:
                        cli.c.send(FramedConnection.MULTIPUT.getBytes());
                        //int multiPutKey = 0; // Variável para armazenar o número inteiro
                        //boolean validMultiPutInput = false;

                        //while (!validMultiPutInput) {
                        //    System.out.println("How many values do you want: ");

                        //    try {
                        //        String line = in.readLine();
                        //        multiPutKey = Integer.parseInt(line); // Tenta converter para inteiro
                        //        validMultiPutInput = true; // Se for bem-sucedido, sai do loop
                        //    } catch (NumberFormatException e) {
                        //        System.out.println("Invalid input. Please enter a valid number.");
                        //    } catch (IOException e) {
                        //        System.out.println("Error reading input: " + e.getMessage());
                        //    }
                        //}
                        //System.out.println("You entered the integer: " + multiGetKey);

                        //Map<String,byte[]> pairs = new HashMap<>();
                        //for (int i = 0; i < multiPutKey; i++) {
                        //    System.out.print("Enter key " + i + ": ");
                        //    String putKey = in.readLine(); // Lê a chave do usuário
                        //    System.out.print("Enter value for key " + i + ": ");
                        //    String putValue = in.readLine(); // Lê o valor do usuário
                        //    pairs.put(putKey, putValue.getBytes());
                        //}

                        //data.multiPut(pairs);

                        break;

                    case 4:
                        cli.c.send(FramedConnection.MULTIGET.getBytes());
                        //int multiGetKey = 0; // Variável para armazenar o número inteiro
                        //boolean validMultiGetInput = false;
                
                        //while (!validMultiGetInput) {
                        //    System.out.println("How many values do you want: ");

                        //    try {
                        //        String line = in.readLine();
                        //        multiGetKey = Integer.parseInt(line); // Tenta converter para inteiro
                        //        validMultiGetInput = true; // Se for bem-sucedido, sai do loop
                        //    } catch (NumberFormatException e) {
                        //        System.out.println("Invalid input. Please enter a number.");
                        //    } catch (IOException e) {
                        //        System.out.println("Error reading input: " + e.getMessage());
                        //    }
                        //}
                        //System.out.println("You entered the integer: " + multiGetKey);

                        //Set<String> keys = new HashSet<>();
                        //for (int i = 0; i < multiGetKey; i++) {
                        //    System.out.print("Enter key : ");
                        //    String getKey = in.readLine(); // Lê a chave do usuário
                        //    keys.add(getKey);
                        //}

                        //Map<String,byte[]> getPairs = new HashMap<>();
                        //getPairs = data.multiGet(pairs);

                        //for (Map.Entry<String, byte[]> entry : getPairs.entrySet()) {
                        //    String tempKey = entry.getKey();       // Obtém a chave
                        //    byte[] tempResult = entry.getValue(); // Obtém o valor (byte[])
                        //    // Verifica e processa o valor
                        //    if (tempResult != null) {
                        //        System.out.println("Key: " + tempKey + ", Value: " + new String(tempResult));
                        //    } else {
                        //        System.out.println("Key: " + tempKey + " has no associated value.");
                        //    }
                        //}
                        break;

                    case 5:
                        cli.c.send(FramedConnection.GETWHEN.getBytes());
                        //System.out.println("A função devolve o valor da primeira chave que meter como input,\n" +
                        //                   "quando o valor da segunda chave que meter indicar for igual ao valor que meter,\n" +
                        //                   "so valtando a ser possivel fazer outras operaccões até tal acontecer");
                        
                        //System.out.print("Enter first key: ");
                        //String firstKey = in.readLine();
                        //System.out.print("Enter second key: ");
                        //String secondKey = in.readLine();
                        //System.out.print("Enter value to compare: ");
                        //String compareValue = in.readLine();


                        //byte[] resultWhen = "null".getBytes();
                        //// result = getWhen(firstKey, secondKey, compareValue);
                        //if (resultWhen != null) {
                        //    System.out.println("Value: " + new String(resultWhen));
                        //}
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
