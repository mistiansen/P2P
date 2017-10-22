import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Client {
	Socket requestSocket;           //socket connect to the server
	OutputStream out;         //stream write to the socket
	InputStream in;          //stream read from the socket

	public void Client() {}


	void run() {
        try{
            requestSocket = new Socket("localhost", 8001);
            Connection connection = new Connection("SERV", requestSocket); //added this
            System.out.println("Connected to localhost in port 8000");
            out = requestSocket.getOutputStream();
            out.flush();
            in = requestSocket.getInputStream();
//
//            try {
//                Thread.sleep(3000);
//            } catch(InterruptedException e) {
//                System.out.println("PROBLEM WITH SLEEP IN CLIENT");
//            }

            if(connection.initiateHandshake("CLIE")){
                System.out.println("HandShake successful");
            } else {
                System.out.println("HandShake failure");
            }


        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
            //Close connections
            try{
                in.close();
                out.close();
                requestSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }


    }


	void runOld()
	{
		try{
			requestSocket = new Socket("localhost", 8001);
			Connection connection = new Connection("SERV", requestSocket); //added this
			System.out.println("Connected to localhost in port 8000");
			out = requestSocket.getOutputStream();
			out.flush();
			in = requestSocket.getInputStream();

			if(connection.initiateHandshake("CLIE")){
			    System.out.println("HandShake successful");
            } else {
                System.out.println("HandShake failure");
            }

			for (int i = 0; i < 4; i++) {
				byte[] length = ByteBuffer.allocate(4).putInt(16).array();
				String message = "P2PHANDSHAKEPROJ";
				byte[] msg = message.getBytes();
				byte type = (byte) 5;
				byte[] payload = ByteBuffer.allocate(16).put(msg).array();

				ByteArrayOutputStream outs = new ByteArrayOutputStream( );
				outs.write(length);
				outs.write(type);
				outs.write(payload);
				byte[] sending = outs.toByteArray();
				String sent = new String(sending);
				System.out.println("Sending this as bytes: " + sent);

				sendMessage(sending);


				byte[] length2 = ByteBuffer.allocate(4).putInt(3).array();
				String message2 = "HEY";
				byte[] msg2 = message2.getBytes();
				byte[] payload2 = ByteBuffer.allocate(3).put(msg2).array();

				ByteArrayOutputStream outs2 = new ByteArrayOutputStream( );
				outs2.write(length2);
				outs2.write(payload2);
				byte[] sending2 = outs2.toByteArray();
				String sent2 = new String(sending2);
				System.out.println("Sending this as bytes: " + sent2);

				sendMessage(sending2);

			}

		}
		catch (ConnectException e) {
    			System.err.println("Connection refused. You need to initiate a server first.");
		}
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	//send a message to the output stream
	void sendMessage(byte[] msg)
	{
		try{
			out.write(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	//main method
	public static void main(String args[])
	{
		Client client = new Client();
		client.run();
	}

}
