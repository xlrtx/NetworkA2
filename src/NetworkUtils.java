import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Common Network Method Implementation Using Blocking TCP Client
 * Note That The Servers Are Single Threaded, Use Of Non-blocking
 * TCP Client Is Meaningless.
 * @author s4337746
 *
 */
public class NetworkUtils implements ProtocolDefs {
  
  
  
  /**
   * Lookup The Address Of Given Name
   * @param name        The Server Name To Lookup
   * @param nsAddr      Name Server Address
   * @return            Address Of Given Server Name
   * @throws Exception  If Fail
   */
  public static InetSocketAddress rpcLookup(String name, InetSocketAddress nsAddr) throws Exception{
    
    
    CachedUDPClient tcpClient = new CachedUDPClient(nsAddr);
    
    
    // Construct Register Request Data
    ByteBuffer requestData = ByteBuffer.allocate(1024);
    requestData.putInt      (RT_NS_REQ_LOOKUP);
    XDRParser.putVarString  (requestData, name);
    requestData.flip();
    
    
    // Send Request, Get Response Data
    ByteBuffer responseData = tcpClient.request(requestData, true, true);
    tcpClient.close();
    
    
    // Check Protocol Number
    if ( responseData.getInt() != RT_NS_RSP_LOOKUP ) {
      throw new Exception();
    }
    
    
    // See If Register Success
    String response = XDRParser.getVarString(responseData);
    if ( response.equals( RSP_LKUP_OK )){
      // Get Address
      String    addr  = XDRParser.getVarString(responseData);
      String[]  split = addr.split(":");
      return new InetSocketAddress(split[0].replace("/", ""), Integer.parseInt(split[1]));
    }else{
      throw new Exception();
    }
    
  }
  
  
  
  
  
  
  /**
   * Register Server Name And Address To Name Server
   * @param name        Server Name To Be Registered
   * @param ip          Server IP To Be Registered
   * @param port        Server Port To Be Registered
   * @throws Exception  If Fail
   */
  public static void rpcRegister(String name, InetSocketAddress myAddr, InetSocketAddress nsAddr) throws Exception{
    

    CachedUDPClient tcpClient = new CachedUDPClient(nsAddr);
    
    
    // Construct Register Request Data
    ByteBuffer requestData = ByteBuffer.allocate(1024);
    requestData.putInt      (RT_NS_REQ_REGISTER);
    XDRParser.putVarString  (requestData, name);
    XDRParser.putVarString  (requestData, myAddr.getHostString() + ":" + myAddr.getPort());
    requestData.flip();
    
    
    // Send Request, Get Response Data
    ByteBuffer responseData = tcpClient.request(requestData, true, true);
    tcpClient.close();
    
    
    // Check Protocol Number
    if ( responseData.getInt() != RT_NS_RSP_REGISTER ) {
      throw new Exception();
    }
    
    
    // See If Register Success
    String response = XDRParser.getVarString(responseData);
    if ( response.equals( RSP_REG_OK )){
      return;
    }else{
      throw new Exception();
    }
    
    
  }
}
