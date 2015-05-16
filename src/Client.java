import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;


public class Client implements ProtocolDefs {

  
  // Stores Server Address
  InetSocketAddress nsAddr;
  InetSocketAddress storeAddr;
  
  
  // Store Connection
  CachedUDPClient storeClient;
  
  
  // Request Type
  int reqType;
  
  
  private final static String MSG_ARGUMENT_ERR      =   "Invalid command line arguments\n";
  private final static String MSG_CONTENT_NOREG     =   "Client unable to connect with NameServer\n";
  private final static String MSG_CONN_STORE_ERR    =   "Client unable to connect with Store\n";
  
  
  public static void main(String[] args){
    
    try {
      new Client(args);
      // TODO some stuff here
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    
    
  }
  
  
  
  /**
   * Client Constructor
   * @param args        Port And Request Type
   * @throws Exception  On Argument Error
   */
  Client(String[] args) throws Exception{


    // Check Argument Count
    int argCount = 0;
    argCount = args.length;
    if (argCount != 2){
      throw new Exception(MSG_ARGUMENT_ERR);
    }
    
    
    // Get Request Type
    try{
      this.reqType = Integer.parseInt(args[0]);
    }catch(Exception e){
      throw new Exception(MSG_ARGUMENT_ERR);
    }
    if ( this.reqType < 0 || this.reqType > 10 ){
      throw new Exception(MSG_ARGUMENT_ERR);
    }
    
    
    // Get Name Server Port
    int nsPort;
    try{
      nsPort = Integer.parseInt(args[1]);
    }catch(NumberFormatException e){
      throw new Exception(MSG_ARGUMENT_ERR);
    }
    this.nsAddr = new InetSocketAddress("127.0.0.1", nsPort);
    
    
    // Lookup Store Address
    try{
      this.storeAddr = NetworkUtils.rpcLookup("Store", this.nsAddr);
    }catch(Exception e){
      // TODO Better Distinguish Fail And Not Exist ------------------
      throw new Exception(MSG_CONTENT_NOREG);
    }
    
    
    // Connect Store
    try{
      this.storeClient = new CachedUDPClient(this.storeAddr);
    }catch(Exception e){
      throw new Exception(MSG_CONN_STORE_ERR);
    }
    
    
    
    if ( this.reqType == 0 ){
      
      HashMap<Long, Double>itemMap = null;
      
      
      // Query For All Items And Print
      try{
        itemMap = rpcQueryItems();
      }catch(Exception e){
        // Note That There Is No Error Message
        throw new Exception();
      }
      
      stdoutItems(itemMap);
      
    } else {
      
      
      // Buy An Item With Item Id And Credit Card Number

      rpcBuyItem( this.reqType, "1111111111111111" );

      
    }

    
  }
  
  
  private void rpcBuyItemErr(Long id){
    System.out.printf("%d transaction aborted\n", id);
  }
  
  
  private void rpcBuyItem(int itemId, String ccn){
    
    
    // Test If Credit Card Number Is Valid
    // TODO: Better Distinguish CCN-Fault And Buy-Fault
    if ( ccn.length() != 16 ){
      rpcBuyItemErr(DUMMY_ITEMID);
    }
    
    // Construct Request Data
    ByteBuffer requestData = ByteBuffer.allocate(1024);
    requestData.putInt(RT_STORE_REQ_BUY);
    
    
    // Put Item Id And Credit Card Number
    requestData.putInt(itemId);
    XDRParser.putFixString( requestData, ccn );
    requestData.flip();
    
    try{
      
      // Send Request
      ByteBuffer responseData = this.storeClient.request(requestData, true, true);
      
      
      // When Wrong Response Packet ID
      if ( responseData.getInt() != RT_STORE_RSP_BUY ) {
        rpcBuyItemErr(DUMMY_ITEMID);
        return;
      }

      
      // When Buy Fails
      if ( !XDRParser.getVarString(responseData).equals(RSP_BUY_OK) ){
        rpcBuyItemErr( Long.parseLong(XDRParser.getFixString(responseData, LEN_ITEMID)) );
        return;
      }
      
      
      // Buy Success, Parse Reply
      Long   id       =   Long.parseLong( XDRParser.getFixString(responseData, LEN_ITEMID) );
      Double price    =   responseData.getDouble();
      String content  =   XDRParser.getVarString(responseData);
      
      System.out.printf("%d ($ %f) CONTENT %s\n", id, price, content);
      
      
    } catch ( Exception e ) {
      
      // TODO: Better Distinguish Others-Fault And Buy-Fault
      rpcBuyItemErr(DUMMY_ITEMID);
    }

    
    
    
  }
  



  HashMap<Long, Double>rpcQueryItems() throws Exception {
    
    
    // Construct Request Data
    ByteBuffer requestData = ByteBuffer.allocate(1024);
    requestData.putInt(RT_STORE_REQ_QUERY);
    requestData.flip();
    
    
    // Send Request
    ByteBuffer responseData = this.storeClient.request(requestData, true, true);
    
    
    // Check Protocol Number
    if ( responseData.getInt() !=  RT_STORE_RSP_QUERY ) {
      throw new Exception();
    }
    
    
    // Get Item Count
    int itemCount = responseData.getInt();
    
    
    // Get Items Out
    HashMap<Long, Double> itemMap = new HashMap<Long, Double>();
    while ( itemCount != 0 ){
      Long      id      =   Long.parseLong( XDRParser.getFixString(responseData, LEN_ITEMID) );
      Double    price   =   responseData.getDouble();
      itemMap.put(id, price);
      itemCount --;
    }
    
    
    return itemMap;
    
  }
  
  
  private void stdoutItems(HashMap<Long, Double> itemMap){
    
    System.out.println("Items Are:");
    Iterator<Entry<Long, Double>> itr = itemMap.entrySet().iterator();
    while(itr.hasNext()){
      Entry<Long, Double> item = itr.next();
      System.out.println(item.getKey() + "  " + item.getValue());
    }
  }
  
}
