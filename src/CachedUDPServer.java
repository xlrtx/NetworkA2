import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;



public class CachedUDPServer implements CachedUDPDefs{
  // Debug mode
  private static boolean DEBUG = true;
  // Server port
  private int port;
  // Cache store
  private HashMap<Integer, Cache> cacheMap;
  // Upper layer callee
  private CachedUDPServerCallback callee;
  
  
  
  
  
  /**
   * The initializer smart UDP server
   * @param servPort
   * @throws IOException
   */
  public CachedUDPServer(CachedUDPServerCallback callee, int servPort) {
    this.port       = servPort;
    this.cacheMap   = new HashMap<Integer, Cache>();
    this.callee     = callee;
  }
  

  
  
  
  /**
   * Handles read, read from client and write.
   * @param key
   * @throws Exception 
   */
  private void handleRead(SelectionKey key) throws Exception{
    
    
    // Get channel from key, key is client, channel is like socket
    DatagramChannel channel = (DatagramChannel) key.channel();
    
    
    // Attachment is customized by ourself, in this case we use
    // attachment to record client's address, return data, etc.
    ClientRecord clntRec = (ClientRecord) key.attachment();
    
    
    // Receive data, record address.
    clntRec.buffer.clear();
    clntRec.clientAddress = channel.receive(clntRec.buffer);
    
    if (clntRec.clientAddress != null) {
      // Got message flip data in.
      clntRec.buffer.flip();
    }else{
      throw new Exception("Recieve failed in handleRead(SelectionKey key).");
    }
    
    
    // Read header
    CachedUDPHeader header = CachedUDPHeader.readHeader(clntRec.buffer);
    if ( !header.isReq() ){
      // This is not a request, ignore this packet.
      return;
    }
    
    
    // This is our response
    ByteBuffer upLyrDataOut = null;
    // Is this response came from cache?
    boolean usingCache = false;
    
    
    // Check if the client agree to use cache
    if ( header.isCache() ){
      
      
      // Client agree to use cached data, 
      // if we have this token cached then return
      // cached data.
      Cache cache = this.cacheMap.get( header.getToken() );
      
      if ( cache != null ){
        // We have the cache, we use it if it is not expired
        if ( !cache.isExpried() ){
          upLyrDataOut  = cache.getData();
          usingCache    = true;
        }else{
          // Data is expired, remove it
          this.cacheMap.remove( header.getToken() );
        }
      }
    }
    
    
    
    if ( upLyrDataOut == null ){
      // Data out is still null, then we have to 
      // generate new response and cache it.
      
      
      // First we need to make sure upper layer
      // don't have access to CachedUDPHeader
      ByteBuffer upLyrDataIn = 
          ByteBuffer.allocate(MAX_PACKET_SIZE - clntRec.buffer.position());
      upLyrDataIn.put(clntRec.buffer);
      upLyrDataIn.flip();
      upLyrDataOut = this.callee.consumeRequest(upLyrDataIn);
      
      
      // Save it to cache
      Cache cache = new Cache(upLyrDataOut);
      this.cacheMap.put( header.getToken(), cache );
      
    }
    
    
    // We got upperlayer's data, create a new pay-load
    ByteBuffer dataOut = CachedUDPUtils.genPayLoad();
    int pktOpt;
    if ( usingCache ) {
      pktOpt = CachedUDPHeader.CU_RSP_CACHE;
    }else{
      pktOpt = CachedUDPHeader.CU_RSP_NOCAC;
    }
    CachedUDPHeader.putHeader(dataOut, pktOpt, header.getToken());
    
    
    // Put upper layer data-out to this layer data-out
    dataOut.put(upLyrDataOut);
    dataOut.flip();
    upLyrDataOut.flip();
    
    
    // Send the data to client
    int bytesSent = channel.send(dataOut, clntRec.clientAddress);
    if (bytesSent != 0) { 
      
    }else{
      
      if ( DEBUG )  System.err.printf("[CachedUDP Log] Notice: No data sent at handle read\n");
      
    }
    
  }
  
  
  
  
  
  
  /**
   * Decay cache, remove all caches that is 
   * too old.
   */
  private void decayCache(){
    
    
    // Loop through all caches and remove the expired one
    Iterator<Entry<Integer, Cache>> itr = 
        this.cacheMap.entrySet().iterator();
    
    
    while( itr.hasNext() ){
      
      Cache cache = itr.next().getValue();
      if ( cache.isExpried() ){
        itr.remove();
      }
      
    }
    
    
  }
  
  
  
  
  
  
  /**
   * Handle selector, three cases are - 
   * read, write, and timeout.
   * @param selector
   * @throws SelectorTimeoutException 
   */
  private void handleSelector( Selector selector ) throws SelectorTimeoutException{
    
    
    try {
      
      
      if (selector.select(SERVER_TIME_OUT) == 0) {
        
        // We have time out, handle timeout, 
        //then throw SelectorTimeoutException to continue loop
        decayCache();
        throw new SelectorTimeoutException("Time out happen.");
        
      }
      
      
    } catch (IOException e) {
      
      System.err.println("Select exception, server shutdown.");
      e.printStackTrace();
      System.exit(1);
      
    }
      
      
    
    // We don't have a timeout, we have read or write instead
    Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
    
    while (keyIter.hasNext()) {
      // Notice that we only have one channel in UDP communication.
      SelectionKey key = keyIter.next(); // Key is bit mask
      
      
      // Read and send
      if (key.isReadable()){
        try {
          handleRead(key);
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      
      
      keyIter.remove();
    }
    
    
  }
  
  
  
  
  /**
   * Main logic of CachedUDPServer
   */
  public void start(){
    
    Selector selector       = null;
    DatagramChannel channel = null;
    
    
    /**
     * Set up server
     */
    try {
      
      
      // Open selector for channel to register
      selector  = Selector.open();
      // Open channel
      channel   = DatagramChannel.open();
      
      
      // Set channel non-blocking
      channel.configureBlocking(false);
      // Bind channel (UDP server) to port
      channel.socket().bind(new InetSocketAddress(this.port));
      // Register channel with the selector
      channel.register(selector, SelectionKey.OP_READ, new ClientRecord());
      
      
    } catch (IOException e) {
      
      System.err.println("Can't start server, shutting down.");
      e.printStackTrace();
      System.exit(1);
      
      
    }
    
    
    
    /**
     * Handle selector, infinite loop
     */
    while (true) {
      
      
      try {
        
        handleSelector( selector );
        
      } catch (SelectorTimeoutException e) {
        
        //if ( DEBUG )  System.err.println("Server time out handled successfuly.");
        continue;
        
      }
      
      
    }

  }
  
  
  
  /**
   * Self-defined selector timeout exception.
   * @author xlrtx
   *
   */
  static class SelectorTimeoutException extends Exception{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public SelectorTimeoutException(String message) {
      super(message);
    }
    
  }
  
  
  
  
  /**
   * Client address and receive data container
   * @author xlrtx
   *
   */
  static class ClientRecord {
    public SocketAddress clientAddress;
    public ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
  }
  
  
  
  

}
