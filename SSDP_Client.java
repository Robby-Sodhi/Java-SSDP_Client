package stockgame.ssdpclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.util.LinkedHashMap;
import java.util.ArrayList;

public class SSDP_Client {
    private static final String MULTICAST_ADDRESS = "239.255.255.250";
    private static final int PORT = 1900;
    private static final int MAX_PACKET_SIZE = 65507; 
    private static final int DEFAULT_TIME_TO_LIVE = 255;
    private static final int DEFAULT_TIMEOUT = 5000;
    
    private String userAgent;
    private String manValue;
    private String st;

    public SSDP_Client() {
        this.userAgent = "Java SSDP Client";
        this.manValue = "ssdp:discover";
        this.st = "ssdp:all";
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setManValue(String manValue) {
        this.manValue = manValue;
    }

    public void setSt(String st) {
        this.st = st;
    }
    
    /**
* @param  timeout how long the method will search for upnp services before timing out IN MILISECONDS. 
* @param  timeToLive how many routers the multicast packet can traverse before failing (I think), between 0-255 inclusive. Default: 255
* @return an ArrayList of all of the discovered services in the format of [{USN, LOCATION, ...}, {...}]
*/
    public ArrayList<LinkedHashMap<String, String>> send(int timeToLive, int timeout) throws IOException {
       ArrayList<LinkedHashMap<String,String>> responses = new ArrayList<LinkedHashMap<String,String>>();

        
        
        // Create the socket
        MulticastSocket socket = new MulticastSocket();
        socket.setReuseAddress(true); //standard practice for ssdp I think
        socket.joinGroup(InetAddress.getByName(MULTICAST_ADDRESS)); //in the ssdp (upnp) standard, all devices are on a multicast group with that address
        
        // Set the time to live
        if (timeToLive < 0)
            timeToLive = DEFAULT_TIME_TO_LIVE;
        socket.setTimeToLive(timeToLive);
        
        
        //set the socket timeout
        if (timeout < 0)
            timeout = DEFAULT_TIMEOUT;
        socket.setSoTimeout(timeout);

        // Create the message
        String message = "M-SEARCH * HTTP/1.1\r\n"
                + "HOST: 239.255.255.250:1900\r\n"
                + "MAN: \"" + manValue + "\"\r\n"
                + "ST: " + st + "\r\n"
                + "MX:" + (timeout / 1000) + "\r\n"
                + "USER-AGENT: " + userAgent + "\r\n\r\n";
        
        // Create the datagram packet
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(MULTICAST_ADDRESS), PORT);

        // Send the packet
        socket.send(packet);

        // Receive responses
        
        try{
        while (true){
            byte[] responseBuffer = new byte[MAX_PACKET_SIZE];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);
            String response = new String(responseBuffer).trim();
            //System.out.println(response);
            //System.out.println();
            
            String[] headerFields = response.split("\r\n");
            String location = null;

            LinkedHashMap<String, String> service = new LinkedHashMap<String, String>();
            for (String headerField : headerFields) {

                String[] parts = headerField.split(":", 2);

                if (parts.length != 2){
                    continue;
                }
                
                
                service.put(parts[0], parts[1]);
               // if (parts[0].equalsIgnoreCase("Location"))
                 //   location = parts[1];
            }


           // if (location != null)
            //   responses.put(location, response);
              responses.add(service);
        
        }
        }catch(java.net.SocketTimeoutException e){
            System.out.println("timed out");
        }
  
    
        
        socket.close();
        return responses;
    }
    
    public ArrayList<LinkedHashMap<String, String>> send() throws IOException{
        return this.send(-1, -1);
    }
}