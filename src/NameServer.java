import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Name Server Implementation
 * @author s4337746
 *
 */
public class NameServer implements CachedUDPServerCallback, ProtocolDefs{
  //TODO: Name server register and lookup function should not throw
  
  //Non-blocking Server To Handle The Requests
  CachedUDPServer myNbServer;
  HashMap<String, InetSocketAddress> serverMap;
  private final static String MSG_ARGUMENT_ERR  =   "Invalid command line arguments for NameServer\n";
  private final static String MSG_BIND_ERR      =   "Cannot listen on given port number ";
  private final static String MSG_BIND_OK       =   "Name Server waiting for incoming connections ...\n";
  
  
  
  public static void main(String[] args) {
    
    NameServer myNameServer = null;
    
    try {
      myNameServer = new NameServer(args);
      System.err.println(MSG_BIND_OK);
      myNameServer.myNbServer.start();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    
    
    
  }
  

  
  /**
   * Name Server Constructor
   * @param args        The port for this name server.
   * @throws Exception  On argument error.
   */
  NameServer(String[] args) throws Exception{
    
   
    
    
    //Initialize Server Map
    this.serverMap = new HashMap<String, InetSocketAddress>();
    
    
    // Check Argument Count
    int argCount = 0;
    argCount = args.length;
    if (argCount != 1){
      throw new Exception(MSG_ARGUMENT_ERR);
    }
    
    
    // Get port number
    int port;
    try{
      port = Integer.parseInt(args[0]);
    }catch(NumberFormatException e){
      throw new Exception(MSG_ARGUMENT_ERR);
    }
    
    
    // Bind Port
    try{
      //Create Non-blocking Server, And Assign Callback.
      this.myNbServer = new CachedUDPServer(this, port);
    }catch(Exception e){
      throw new Exception(MSG_BIND_ERR + port  + "\n");
    }
    

  }

  
  
  
  
  
  /**
   * Register Server Name With It's Address
   * @param name
   * @param address
   * @throws UnsupportedEncodingException 
   */
  private ByteBuffer register ( ByteBuffer requestData ) throws Exception{
    
    
    //Parse Data
    String name = XDRParser.getVarString(requestData);
    String addr = XDRParser.getVarString(requestData);
    String[] split = addr.split(":");
    InetSocketAddress address = 
        new InetSocketAddress(split[0], Integer.parseInt(split[1]));
    
    
    
    //Save To Map
    String returnMsg = "";
    if ( this.serverMap.containsKey(name) ){
      returnMsg = RSP_REG_NOTOK;
    }else{
      this.serverMap.put(name, address);
      returnMsg = RSP_REG_OK;
    }
    
    
    //Reconstruct Reply Data
    ByteBuffer responseData = ByteBuffer.allocate(1024);
    responseData.putInt(RT_NS_RSP_REGISTER);
    XDRParser.putVarString(responseData, returnMsg);
    responseData.flip();
    
    
    return responseData;
    
  }

  
  
  
  
  
  /**
   * Lookup Server's Address With Given Name
   * @param requestData
   * @return
   * @throws Exception
   */
  private ByteBuffer lookup ( ByteBuffer requestData ) throws Exception{
    
    //Parse Data
    String name = XDRParser.getVarString(requestData);
    InetSocketAddress address = this.serverMap.get(name);
    
    
    //Lookup Map
    boolean lookupOk;
    String returnMsg = "";
    if(address == null){
      returnMsg = RSP_LKUP_NOTOK;
      lookupOk = false;
    }else{
      returnMsg = RSP_LKUP_OK;
      lookupOk = true;
    }
    
    
    //Reconstruct Reply Data
    ByteBuffer responseData = ByteBuffer.allocate(1024);
    responseData.putInt(RT_NS_RSP_LOOKUP);
    XDRParser.putVarString(responseData, returnMsg);
    if ( lookupOk ){
      XDRParser.putVarString(responseData, address.getAddress() + ":" + address.getPort());
    }
    responseData.flip();
    
    
    return responseData;
    
  }

  
  

  
  /**
   * Consume Request And Generate Response Data, NbServer Callback.
   * @param requestData
   * @return
   * @throws Exception
   */
  @Override
  public ByteBuffer consumeRequest(ByteBuffer requestData) throws Exception{
    
    int requestType = requestData.getInt();
    
    if( requestType == RT_NS_REQ_LOOKUP ){
      return lookup(requestData);
      
      
    }else if( requestType == RT_NS_REQ_REGISTER ){
      return register(requestData);
      
    }else{
      // Throw Error When Illegal Packet, Connection Will Be Closed.
     throw new Exception("no such method for this server");
    }

  }
  
}