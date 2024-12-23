package connection;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;


public class FramedConnection implements AutoCloseable{
    private Socket sock;
    private DataInputStream input;
    private DataOutputStream output;
    private ReentrantLock outputLock = new ReentrantLock();
    private ReentrantLock inputLock = new ReentrantLock();
    public static final String REGISTER = "R\0";
    public static final String LOGIN = "L\0";
    public static final String CHANGEPASSWORD = "CP";
    public static final String NEXTSTEP = "N\0";
    public static final String PUT = "P\0";
    public static final String GET = "G\0";
    public static final String MULTIPUT = "MP";
    public static final String MULTIGET = "MG";
    public static final String GETWHEN = "GH";
    public static final String SUCCESS = "200"; 


    public FramedConnection(Socket Socket) throws IOException {
        this.sock = Socket;
        this.input = new DataInputStream(new BufferedInputStream(this.sock.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(this.sock.getOutputStream()));
    }

    public FramedConnection() throws IOException {
        this.sock = new Socket("localhost",12345);
        this.input = new DataInputStream(new BufferedInputStream(this.sock.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(this.sock.getOutputStream()));
    }

    public void send(byte[] data) throws IOException { 
        this.outputLock.lock();
        try{
            this.output.writeInt(data.length);
            this.output.write(data);
            this.output.flush();
        } finally {
            this.outputLock.unlock();
        }
    }

    public byte[] receive() throws IOException { 
        this.inputLock.lock();
        try{
            int size = this.input.readInt();
            byte[] data = new byte[size];
            this.input.readFully(data);
            //System.err.println("Read the reply");
            //System.out.println(data);
            return data;
        } finally {
            this.inputLock.unlock();
        }
    }

    public void close() throws IOException {
        this.sock.close();
    }

    public boolean isClosed(){
        return this.sock.isClosed();
    }
}