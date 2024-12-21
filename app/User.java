import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import client.Client;
import connection.FramedConnection;

public class User {

    public static Client parseLine(String userInput) {
        String[] tokens = userInput.split(" ");
        return new Client(tokens[0], tokens[1]);
    }

    /*modificar para o que ser quer */
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 12345);
        FramedConnection c = new FramedConnection(socket);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        
        /*
        DataInputStream ctList = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        ClientList clientList = ClientList.deserialize(ctList);
        */

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
                    case 1:
                        System.out.print("Enter key: ");
                        String key = in.readLine();
                        System.out.print("Enter value: ");
                        String value = in.readLine();
                        //data.put(key, value.getBytes());
                        break;

                    case 2:
                        System.out.print("Enter key: ");
                        key = in.readLine();
                        byte[] result = "null".getBytes();
                        // result = data.get(key);
                        if (result != null) {
                            System.out.println("Value: " + new String(result));
                        }
                        break;
                    
                    case 3:
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
                
                            // Envia os dados para o servidor
                            c.send(username.getBytes());
                            c.send(password.getBytes());
                
                            // Recebe a resposta do servidor
                            byte[] reply = c.receive();
                            String typeOfReply = new String(reply);
                
                            // Verifica o tipo de resposta
                            if (typeOfReply.equals("1")) {
                                System.out.println("Incorrect password. Please try again.");
                            } else if (typeOfReply.equals("2")) {
                                System.out.println("Incorrect password or username. Please try again.");
                            } else if (typeOfReply.equals("0")) {
                                System.out.println("Login successful!");
                                updatePassword = false; // Sai do loop quando o login for bem-sucedido
                            } else {
                                System.out.println("Unexpected reply from server: " + typeOfReply);
                            }
                            tries++;
                        }

                        if(updatePassword == false) {
                            
                        }
                        break;

                    case 4:
                        System.out.println("Exiting...");
                        valid = false;
                        c.close();
                        break;

                    default:
                        System.out.println("Invalid choice.");
                }
            }

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
                        System.out.print("Enter key: ");
                        String key = in.readLine();
                        System.out.print("Enter value: ");
                        String value = in.readLine();
                        //data.put(key, value.getBytes());
                        break;

                    case 2:
                        System.out.print("Enter key: ");
                        key = in.readLine();
                        byte[] result = "null".getBytes();
                        // result = data.get(key);
                        if (result != null) {
                            System.out.println("Value: " + new String(result));
                        }
                        break;
                    
                    case 3:
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
                        //System.out.println("You entered the integer: " + multiGetKey);

                        Map<String,byte[]> pairs = new HashMap<>();
                        for (int i = 0; i < multiPutKey; i++) {
                            System.out.print("Enter key " + i + ": ");
                            String putKey = in.readLine(); // Lê a chave do usuário
                            System.out.print("Enter value for key " + i + ": ");
                            String putValue = in.readLine(); // Lê o valor do usuário
                            pairs.put(putKey, putValue.getBytes());
                        }

                        //data.multiPut(pairs);

                        break;

                    case 4:
                        int multiGetKey = 0; // Variável para armazenar o número inteiro
                        boolean validMultiGetInput = false;
                
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
                        //System.out.println("You entered the integer: " + multiGetKey);

                        Set<String> keys = new HashSet<>();
                        for (int i = 0; i < multiGetKey; i++) {
                            System.out.print("Enter key : ");
                            String getKey = in.readLine(); // Lê a chave do usuário
                            keys.add(getKey);
                        }

                        Map<String,byte[]> getPairs = new HashMap<>();
                        //getPairs = data.multiGet(pairs);

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

                    case 5:
                        System.out.println("A função devolve o valor da primeira chave que meter como input,\n" +
                                           "quando o valor da segunda chave que meter indicar for igual ao valor que meter,\n" +
                                           "so valtando a ser possivel fazer outras operaccões até tal acontecer");
                        
                        System.out.print("Enter first key: ");
                        String firstKey = in.readLine();
                        System.out.print("Enter second key: ");
                        String secondKey = in.readLine();
                        System.out.print("Enter value to compare: ");
                        String compareValue = in.readLine();


                        byte[] resultWhen = "null".getBytes();
                        // result = getWhen(firstKey, secondKey, compareValue);
                        if (resultWhen != null) {
                            System.out.println("Value: " + new String(resultWhen));
                        }
                        break;

                    case 6:
                        System.out.println("Exiting...");
                        running = false;
                        c.close();
                        break;

                    default:
                        System.out.println("Invalid choice.");
                }
            }
        
        /*
        String userInput;
        while ((userInput = in.readLine()) != null) {
            Client newClient = parseLine(userInput);
            System.out.println(newClient.toString());
            newClient.serialize(out);
            out.flush();
        }
        */
        
        socket.close();
    }
}
