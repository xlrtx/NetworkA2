import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * An Utility Class For XDR Encoding And Parsing
 * @author s4337746
 *
 */

//TODO: String length check.
public class XDRParser {
  
  public static String getFixString(ByteBuffer buffer, int length) throws UnsupportedEncodingException{
    
    //Get String Raw Data
    byte[] stringBuffer = new byte[length];
    buffer.get(stringBuffer);
    
    return new String(stringBuffer, "UTF-8");
    
  }
  
  
  public static ByteBuffer putFixString(ByteBuffer buffer, String str){
    
    //Put String
    byte[] stringBuffer = str.getBytes(Charset.forName("UTF-8"));
    buffer.put(stringBuffer);
    
    return buffer;
    
  }
  
  
  
  public static String getVarString(ByteBuffer buffer) throws UnsupportedEncodingException{
    
    //Get String Length
    int length = buffer.getInt();
    
    //Get String Raw Data
    byte[] stringBuffer = new byte[length];
    buffer.get(stringBuffer);
    
    
    return new String(stringBuffer, "UTF-8");
    
  }
  
  public static ByteBuffer putVarString(ByteBuffer buffer, String str){
    
    //Get String Length
    int length = str.length();
    
    //Put Length
    buffer.putInt(length);
    
    //Put String
    byte[] stringBuffer = str.getBytes(Charset.forName("UTF-8"));
    buffer.put(stringBuffer);
    
    return buffer;
    
  }
  
}
