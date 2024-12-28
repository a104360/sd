package tests;

import client.Client;
import connection.FramedConnection;

import java.io.IOException;
//import java.util.List;

public class ClientTest extends Client{
    
    public ClientTest(FramedConnection c)throws IOException{
        super("j", "j", c);
    }

    public static void main(String[] args){
        int size = Integer.parseInt(args[0]);

        Thread t = new Thread(()->{
            try{
                FramedConnection c = new FramedConnection();
                Client cli = new Client("j","j",c);
    
                c.send(FramedConnection.LOGIN.getBytes());
                c.send(cli.getName().getBytes());        
                c.send(cli.getPassword().getBytes());        
                String reply = new String(c.receive());
                c.send(FramedConnection.NEXTSTEP.getBytes());
    
                c.receive();
    
            long start = System.nanoTime();
            for(int i = 0;i < size;i++){
                //cli.put("b", "benfica".getBytes());
                c.send(FramedConnection.GET.getBytes());
                c.send(Integer.toString(i).getBytes());
                c.receive();
                //c.send(Integer.toString(i).getBytes());
                //System.out.println(i);
                //System.out.println(new String(c.receive()));
            }
            long end = System.nanoTime();
            System.out.println("START : " + start);
            System.out.println("END : " + end);
            System.out.println("j Throught put : " + ((end-start)*0.000000001));
            c.close();
                //c.testeClient();
            } catch (IOException e){
                e.printStackTrace();
            }

        });
        Thread t2 = new Thread(()->{
            try{
                FramedConnection c = new FramedConnection();
                Client cli = new Client("c","c",c);
    
                c.send(FramedConnection.LOGIN.getBytes());
                c.send(cli.getName().getBytes());        
                c.send(cli.getPassword().getBytes());        
                String reply = new String(c.receive());
                c.send(FramedConnection.NEXTSTEP.getBytes());
    
                c.receive();
    
            long start = System.nanoTime();
            for(int i = 0;i < size;i++){
                //cli.put("b", "benfica".getBytes());
                c.send(FramedConnection.PUT.getBytes());
                c.send(Integer.toString(i).getBytes());
                c.send(Integer.toString(i).getBytes());
                //System.out.println(i);
                //System.out.println(new String(c.receive()));
            }
            long end = System.nanoTime();
            System.out.println("START : " + start);
            System.out.println("END : " + end);
            System.out.println("c Throught put : " + ((end-start)*0.000000001));
            c.close();
                //c.testeClient();
            } catch (IOException e){
                e.printStackTrace();
            }

        });
        
        t.start();
        t2.start();
    }
}
