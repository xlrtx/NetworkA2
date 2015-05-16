import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


public class Bank implements CachedUDPServerCallback, ProtocolDefs{

  //Non-blocking Server To Handle The Requests
  CachedUDPServer myNbServer;
  
  
  // Stores Server Address
  InetSocketAddress myAddr;
  InetSocketAddress nsAddr;
  
  
  private final static String MSG_ARGUMENT_ERR  =   "Invalid command line arguments for Bank\n";
  private final static String MSG_REGISTER_ERR  =   "Bank registration to NameServer failed\n";
  private final static String MSG_BIND_ERR      =   "Bank unable to listen on given port\n";
  private final static String MSG_BIND_OK       =   "Bank waiting for incoming connections\n";
  
  private final static String MSG_TRANS_OK      =   "OK";
  private final static String MSG_TRANS_NOTOK   =   "NOT OK";
  
  public static void main(String[] args) {
    
    Bank myBank = null;
    
    try {
      myBank = new Bank(args);
      System.err.println(MSG_BIND_OK);
      myBank.myNbServer.start();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    
    
    
  }
  

  
  /**
   * Bank Constructor
   * @param args        The port for this name server.
   * @throws Exception  On argument error.
   */
  Bank(String[] args) throws Exception{
    
    
    
    // Check Argument Count
    int argCount = 0;
    argCount = args.length;
    if (argCount != 2){
      throw new Exception(MSG_ARGUMENT_ERR);
    }
    
    
    // Get Port
    int port;
    try{
      port = Integer.parseInt(args[0]);
    }catch(NumberFormatException e){
      throw new Exception(MSG_ARGUMENT_ERR);
    }
    this.myAddr = new InetSocketAddress("127.0.0.1", port);
    
    
    // Get Name Server Port
    int nsPort;
    try{
      nsPort = Integer.parseInt(args[1]);
    }catch(NumberFormatException e){
      throw new Exception(MSG_ARGUMENT_ERR);
    }
    this.nsAddr = new InetSocketAddress("127.0.0.1", nsPort);
    
    
    // Register MySelf To Name Server
    try{
      NetworkUtils.rpcRegister("Bank", this.myAddr, this.nsAddr);
    }catch(Exception e){
      throw new Exception(MSG_REGISTER_ERR);
    }
    
    
    // Bind Port
    try{
      this.myNbServer = new CachedUDPServer(this, this.myAddr.getPort());
    }catch(Exception e){
      throw new Exception(MSG_BIND_ERR);
    }
    

  }

  

  /**
   * The transaction handler
   * @param requestData     Data send from client
   * @return                Data send to   client
   * @throws Exception
   */
  private ByteBuffer transaction(ByteBuffer requestData){

    Long    id = null;
    Double  price;
    String  ccn;
    
    
    // Return Message
    String retMsg = "";
    
    
    try{
      
      
      // Parse Data
      id      =   Long.parseLong( XDRParser.getFixString(requestData, 10) );
      price   =   requestData.getDouble();
      ccn     =   XDRParser.getFixString(requestData, 16);
      System.out.printf("Bank:   Trans request, id = %d, price = %f, cnn = %s\n", id, price, ccn);
      
      
      // Check Item Id
      if ( id % 2 == 0 ){
        
        // Even
        retMsg = RSP_TRANS_NOTOK;
        System.out.println(MSG_TRANS_NOTOK);
        
      } else {
        
        // Odd
        retMsg = RSP_TRANS_OK;
        System.out.println(MSG_TRANS_OK);
        
      }
      
      
    } catch ( Exception e ){
      
      retMsg = RSP_TRANS_NOTOK;
      System.out.println(MSG_TRANS_NOTOK);
      
    }

    
    // Construct Response Data
    ByteBuffer responseData = ByteBuffer.allocate(1024);
    responseData.putInt(RT_BANK_RSP_TRANS);
    XDRParser.putVarString(responseData, retMsg);
    
    
    // Flip The Data
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
    
    if( requestType == RT_BANK_REQ_TRANS ){
      return transaction(requestData);
    }else{
      // Throw Error When Illegal Packet, Connection Will Be Closed.
     throw new Exception("no such method for this server");
    }
    
  }


  

}
