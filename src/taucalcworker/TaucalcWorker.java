/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package taucalcworker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;


/**
 *
 * @author Mark
 */
public class TaucalcWorker {

public static final String WORKQUEUENAME = "TauWorkQueue";
public static final String RESULTQUEUENAME = "TauResultQueue";
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.util.concurrent.TimeoutException
     */
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(WORKQUEUENAME, false, false, false, null);
        channel.queueDeclare(RESULTQUEUENAME, false, false, false, null);
        channel.basicQos(1);
        
        byte[] inputbyte = {0, 0, 0, 0};
        String input = "";
        
        do {
            if(System.in.available()>0) {
                System.in.read(inputbyte);
                input = new String(inputbyte);
            }
            GetResponse response = channel.basicGet(WORKQUEUENAME, false);
            if(response != null) {
                long deliverytag = response.getEnvelope().getDeliveryTag();
                byte[] body = response.getBody();
                int tries = ByteBuffer.wrap(body).getInt();
                System.out.println("Task received: " + tries);
                int success = 0;
                for(int i = 0; i < tries; i++) {
                    double x = Math.random();
                    double y = Math.random();
                    if(x*x + y*y <= 1) {success++;}
                }
                System.out.println("success: " + success + " out of " + tries);
                double tau = ((double)success/tries)*8;
                System.out.println("Tau = " + tau);
                byte[] resultbytes = new byte[8];
                ByteBuffer.wrap(resultbytes).putDouble(tau);
                channel.basicPublish("", RESULTQUEUENAME, null, resultbytes);
                channel.basicAck(deliverytag, false);
            }
        }while(!input.equals("stop"));
        channel.close();
        connection.close();
        System.out.println("You stopped the program.");
    }
    

}
