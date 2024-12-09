import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

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

        System.out.print("Username: ");
        String username = in.readLine();
        System.out.print("Password: ");
        String password = in.readLine();

        c.send(username.getBytes());
        c.send(password.getBytes());
        
        byte[] reply = c.receive();
        //System.out.println("Não passou do receber");
        //System.out.println(reply.length);
        System.out.print(new String(reply));


        boolean running = true;
            while (running) {
                System.out.println("\n--- Main Menu ---");
                System.out.println("1. Put (store a value)");
                System.out.println("2. Get (retrieve a value)");
                System.out.println("3. Exit");
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
                        //byte[] result = data.get(key);
                        //if (result != null) {
                        //    System.out.println("Value: " + new String(result));
                        //}
                        break;

                    case 3:
                        System.out.println("Exiting...");
                        running = false;
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
