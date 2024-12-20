package client;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class Client {
    private String name;
    private String password;

    public Client(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public String getName() { 
        return name; 
    }

    public String getPassword() { 
        return password; 
    }

    /**
     * Writes the bytes from a client credentials to the DataOutputStream passed as an argument
     * @param out
     * @throws IOException
     */
    public void serialize(DataOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(password);
        //flush fora para mais controlo
    }

    public static Client deserialize(DataInputStream in) throws IOException { 
        String name;
        name = in.readUTF();
        
        String password;
        password = in.readUTF();

        return new Client(name, password);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.name).append(";");
        builder.append(this.password);
        builder.append("}");
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
        return Objects.equals(name, other.getName()) && Objects.equals(password, other.getPassword());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, password);
    }

}
