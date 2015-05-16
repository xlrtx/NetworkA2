import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;


public class Content implements CachedUDPServerCallback, ProtocolDefs{

  
  //Non-blocking Server To Handle The Requests
  CachedUDPServer myNbServer;
  
  
  // Stores Server Address
  InetSocketAddress myAddr;
  InetSocketAddress nsAddr;
  
  HashMap<Long, String>contentMap;
  private final static String MSG_ARGUMENT_ERR  =   "Invalid command line arguments for Content\n";
  private final static String MSG_REGISTER_ERR  =   "Content registration with NameServer failed\n";
  private final static String MSG_BIND_ERR      =   "Content unable to listen on given port\n";
  private final static String MSG_BIND_OK       =   "Content waiting for incoming connections\n";
  
  
  public static void main(String[] args) {
    
    Content myContent = null;
    
    try {
      myContent = new Content(args);
      System.err.println(MSG_BIND_OK);
      myContent.myNbServer.start();
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
  Content(String[] args) throws Exception{
    
    
    this.contentMap = new HashMap<>();
    
    // Check Argument Count
    int argCount = 0;
    argCount = args.length;
    if (argCount != 3){
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
    
    
    // Get Stock File Path And Load To Map
    String path = args[1];
    try{
      loadContentFile(path);
    }catch(Exception e){
      throw new Exception(MSG_ARGUMENT_ERR);
    }
    
    
    // Get Name Server Port
    int nsPort;
    try{
      nsPort = Integer.parseInt(args[2]);
    }catch(NumberFormatException e){
      throw new Exception(MSG_ARGUMENT_ERR);
    }
    this.nsAddr = new InetSocketAddress("127.0.0.1", nsPort);
    
    
    // Register MySelf To Name Server
    try{
      NetworkUtils.rpcRegister("Content", this.myAddr, this.nsAddr);
    }catch(Exception e){
      throw new Exception(MSG_REGISTER_ERR);
    }
    
    
    // Bind Port
    try{
      //Create Non-blocking Server, And Assign Callback.
      this.myNbServer = new CachedUDPServer(this, this.myAddr.getPort());
    }catch(Exception e){
      throw new Exception(MSG_BIND_ERR);
    }
    

  }


  
  
  /**
   * Load Content File To Map
   * @param path            The File Path
   * @throws Exception 
   */
  private void loadContentFile(String path) throws Exception{
    
    
    BufferedReader br = new BufferedReader(new FileReader(path));
    try {
      String line = br.readLine();
      while (line != null) {
        
        //Ignore Empty Line
        if( line.equals("") ){
          continue;
        }
        
        String[] split = line.split(" ");
        
        //Id Must Be 10 Digits
        if( split[0].length() != LEN_ITEMID ){
          throw new Exception();
        }
        
        Long    itemId        =   Long.   parseLong   ( split[0] );
        String  itemContent   =   split[1];
        this.contentMap.put(itemId, itemContent);
        
        line = br.readLine();
        
      }
    } finally {
      
      br.close();
      
    }
  }
  
  
  
  /**
   * Fetch Content Data
   * @param requestData
   * @return
   */
  public ByteBuffer fetch( ByteBuffer requestData ) {
    
    
    // Construct Response Data
    ByteBuffer responseData = ByteBuffer.allocate(1024);
    responseData.putInt(RT_CONT_RSP_FETCH);
    
    
    try{
      
      // Parse Request Data
      Long id = Long.parseLong( XDRParser.getFixString(requestData, LEN_ITEMID) );

      
      // Get Item Content From Map
      String content = this.contentMap.get(id);
      if ( content == null ) {
        throw new Exception();
      }
      
      
      // Fill Up Packet
      XDRParser.putVarString(responseData, RSP_FETCH_OK);
      XDRParser.putFixString(responseData, id.toString());
      XDRParser.putVarString(responseData, content);
      responseData.flip();
      
    } catch ( Exception e ) {
      
      XDRParser.putVarString(responseData, RSP_FETCH_NOTOK);
      responseData.flip();
      
    }
    
    
    return responseData;
    
  }
  
  

  @Override
  public ByteBuffer consumeRequest(ByteBuffer requestData) throws Exception {
    
    
    int requestType = requestData.getInt();
    
    if( requestType == RT_CONT_REQ_FETCH ){
      return fetch(requestData);
    }else{
      // Throw Error When Illegal Packet, Connection Will Be Closed.
     throw new Exception("no such method for this server");
    }
  }
  
}
