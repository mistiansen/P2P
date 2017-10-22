import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;


public class Server {

    private static final int sPort = 8001;   //The server will be listening on this port number

    public static void main(String[] args) throws Exception {
        System.out.println("The server is running.");
        ServerSocket listener = new ServerSocket(sPort);
        int clientNum = 1;
        try {
            while (true) {
                new Handler(listener.accept(), clientNum).start();
                System.out.println("Client " + clientNum + " is connected!");
                clientNum++;
            }
        } finally {
            listener.close();
        }

    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    private static class Handler extends Thread {
        private Socket socket;
        private Connection connection;
        private InputStream in;    //stream read from the socket
        private OutputStream out;    //stream write to the socket
        private int no;        //The index number of the client

        public Handler(Socket socket, int no) {
            this.socket = socket;
            this.no = no;
            this.connection = new Connection("CLIE", socket); //added this
        }

        public void run() {
            try {
                out = socket.getOutputStream();
                out.flush();
                in = socket.getInputStream();

                if (this.connection.reciprocateHandshake("SERV")) {
                    System.out.println("Successfully shook hands with client");
                } else {
                    System.out.println("Handshake with client failed");
                }


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //Close connections
                try {
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException ioException) {
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }

        public void runOld() {
            try {
                out = socket.getOutputStream();
                out.flush();
                in = socket.getInputStream();
//                byte[] b = new byte[4]; // looks like anything declared outside of the loop keeps the message in the byte array
//                byte[] message = new byte[16]; // looks like anything declared outside of the loop keeps the message in the byte array
//                    while (true) {
                for (int i = 0; i < 5; i++) {
                        byte[] b = new byte[4]; // looks like anything declared outside of the loop keeps the message in the byte array

                        in.read(b, 0, 4);
                        int type = in.read();
//                        String len = new String(b);
                        int length = Util.bytesToInt(b);
                        String len = Integer.toString(length);
                        byte[] message = new byte[length]; //need to consider the
//                        byte[] message = ByteBuffer.allocate(length).array();
                        String mess = new String();
                        try {
                            in.read(message, 0, 16);
                            mess = new String(message);
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("message " + i + " length: " + len + " type: " + type);
                        System.out.println("Rest of message " + i + " : " + mess);
                        System.out.println(" ");

                    in.read(b, 0, 4);
                    int length2 = Util.bytesToInt(b);
                    byte[] message2 = new byte[length2]; //need to consider the
                    String mess2 = new String();
                    try {
//                            message = this.in.readAllBytes(); //will this also read in the message length field again?
                        in.read(message2, 0, 3);
                        mess2 = new String(message2);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("2ng message " + i + " length: " + length2);
                    System.out.println("Rest of 2nd message " + i + " : " + mess2);
                    System.out.println(" ");

                    }
            } catch (IOException ioException) {
                System.out.println("Disconnect with Client " + no);
            } finally {
                //Close connections
                try {
                    in.close();
                    out.close();
                    connection.close();
                } catch (IOException ioException) {
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }

        //send a message to the output stream
        public void sendMessage(byte[] msg) {
            try {
                out.write(msg);
                out.flush();
                System.out.println("Send message: " + msg + " to Client " + no);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }

}
