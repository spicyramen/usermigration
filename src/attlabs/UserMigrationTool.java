package attlabs;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;


/**
 * AT&T Labs
 * Author: 	Gonzalo Gasca Meza
 * Date: 	August 2014
 * Purpose: Perform userId Migration via AXL SOAP
 */

public class UserMigrationTool {
	
	static private int CONSTANT_USERLIMIT = 1000;
	static private int SLEEP_TIME = 1500;
	    
    /** Defines the connection to AXL for the test driver */
    private SOAPConnection soapConnection;

    /** DOCUMENT ME! */
    private String port = "8443";

    /** DOCUMENT ME! */
    private String host = "1.1.1.1";
    private String username = "admin";
    private String password = "admin";
    private String outputFile = "migration.log";
    private String userFile = "users.csv";
    
    private ArrayList<User> usersToBeMigrated = null;
    private int migrationErrors = 0;
   
    

    /**
     * This method provides the ability to initialize the SOAP connection
     */
    private void init() {
        try {
            X509TrustManager xtm = new MyTrustManager();
            TrustManager[] mytm = { xtm };
            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(null, mytm, null);
            SSLSocketFactory sf = ctx.getSocketFactory();

            SOAPConnectionFactory scf = SOAPConnectionFactory.newInstance();
            soapConnection = scf.createConnection();

            HttpsURLConnection.setDefaultSSLSocketFactory(sf);
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method provides the ability to convert a SOAPMessage to a String
     *
     * @param inMessage SOAPMessage
     * @return String The string containing the XML of the SOAPMessage
     * @throws Exception Indicates a problem occured during processing of the SOAPMessage
     */
    public static String convertSOAPtoString(SOAPMessage inMessage) throws Exception {
        Source source = inMessage.getSOAPPart().getContent();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        ByteArrayOutputStream myOutStr = new ByteArrayOutputStream();
        StreamResult res = new StreamResult();
        res.setOutputStream(myOutStr);
        transformer.transform(source, res);
        return myOutStr.toString().trim();
    }

    
    /**
     */
    public SOAPMessage createUpdateUser(String cmdName, String OlduserId, String newUserId) throws Exception {
        // Add a soap body element to the soap body
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage soapMessage = mf.createMessage();
        MimeHeaders mimeHeader = soapMessage.getMimeHeaders();
        SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
        mimeHeader.setHeader("SOAPAction","CUCM:DB ver=9.1 "+ cmdName); 
        envelope.addNamespaceDeclaration("ns","http://www.cisco.com/AXL/API/9.1");
        SOAPBody bdy = envelope.getBody();        
        SOAPBodyElement bodyElement = bdy.addBodyElement(envelope.createName("ns:" + cmdName));
        bodyElement.addChildElement("userid").addTextNode(OlduserId);
        bodyElement.addChildElement("newUserid").addTextNode(newUserId);
        return soapMessage;
    }
    
    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public String getUrlEndpoint() {
        return new String("https://" + username + ":" + password + "@" + host + ":" + port + "/axl/");
    }

    /**
     * This method provides the ability to send a specific SOAPMessage
     * 
     * @param requestMessage The message to send
     */
    public boolean sendMessage(SOAPMessage requestMessage) throws Exception {
        SOAPMessage reply = null;
        boolean result = false;
        try {
            System.out.println("*****************************************************************************");
            System.out.println("Sending SOAP message...");
            System.out.println("---------------------");
            requestMessage.writeTo(System.out);
            System.out.println("\n---------------------");

            reply = soapConnection.call(requestMessage, getUrlEndpoint());

            if (reply != null) {
                //Check if reply includes soap fault
                SOAPPart replySP = reply.getSOAPPart();
                SOAPEnvelope replySE = replySP.getEnvelope();
                SOAPBody replySB = replySE.getBody();

                if (replySB.hasFault()) {
                    System.out.println("ERROR: " + replySB.getFault().getFaultString());
                    result = false;
                }
                else {
                    System.out.println("Succesful response received.");
                    result = true;
                }
                System.out.println("---------------------");
                reply.writeTo(System.out);
                FileWriter fw = new FileWriter(outputFile, true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write("---------------------------- " + requestMessage.getSOAPBody().getTextContent() + " ----------------------------");
                bw.newLine();
                bw.write(convertSOAPtoString(reply));
                bw.newLine();
                bw.flush();
                bw.close();
                System.out.println("\n---------------------");
            }
            else {
                System.out.println("No reply was received!");
                System.out.println("---------------------");
                result = false;
            }

            System.out.println("");
        }
        catch (Exception e) {
        	result = false;
            e.printStackTrace();
            throw e;
            
        }

        return result;
    }

    private void migration() {
    	
    	displayParameters();
    	validateUserSize(usersToBeMigrated.size());
    	pressAnyKeyToContinue();
    	System.out.println("DEBUG: Starting migration!");
    	
    	long startTime = System.currentTimeMillis();
    	
    	for (int i=0;i< usersToBeMigrated.size();i++) {
			User entry = usersToBeMigrated.get(i);
			System.out.println("DEBUG: Migrating userId [" + entry.getOldUserId() + "] to new userId [" + entry.getNewUserId() + "]");
			execute(1,entry.getOldUserId(),entry.getNewUserId());
		}
    	
    	System.out.println();
    	System.out.println("Migration completed. Total: " + migrationErrors + " errors found");
    	long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Migration took " + elapsedTime + " milliseconds to complete");
    }
    
    
    /**
     * 
     */
    private void displayParameters() {
    	System.out.println("CUCM configuration parameters:");
    	System.out.println("*****************************************************************************");   
    	System.out.println("DEBUG: CUCM Hostname: " + host);
    	System.out.println("DEBUG: CUCM username: " + username);
    	System.out.println("DEBUG: CUCM password: " + password);
    	System.out.println("DEBUG: User File: " + userFile);
    	System.out.println("DEBUG: Total users ready to migrate: " + usersToBeMigrated.size());
    	System.out.println("*****************************************************************************");
    
		
	}

	/**
     * 
     * @param size
     */
    private void validateUserSize(int size) {
		if (size > CONSTANT_USERLIMIT) {
			System.out.println("ERROR: Total users found: " + usersToBeMigrated.size() +  " exceed permit limit. Limit: " + CONSTANT_USERLIMIT);
			System.exit(1);
		}
		
	}

	/**
     * This method provides the ability to execute the unit testing
     */
    private void execute(int type,String... params) {
    	boolean result = false;
    	
    	try {
    		if(type == 1) {			
    			result = sendMessage(createUpdateUser("updateUser",params[0],params[1]));
    			if (result == false) {
    				System.out.println("ERROR: Error migrating user: " + params[0]);
    				migrationErrors++;
    			}
    			Thread.sleep(SLEEP_TIME);
    		}
    		else {
    			System.out.println("ERROR: Invalid SOAP method");
    		}
    		
    	
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class GenericNodeFilter implements NodeFilter {
        String theNodeName = null;

        public GenericNodeFilter(String _nodeName) {
            theNodeName = _nodeName;
        }

        public short acceptNode(Node _node) {
            if (_node.getNodeName().equals(theNodeName)) {
                return NodeFilter.FILTER_ACCEPT;
            }
            else {
                return NodeFilter.FILTER_REJECT;
            }
        }
    }
    /**
     * 
     * @param fileName
     */
    private void processCsvFile() {
    	 ReadCsvFile csvFile = new ReadCsvFile(userFile);
    	 
         try {
			csvFile.ReadFileEntries();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
         //csvFile.printAll();
         usersToBeMigrated = csvFile.getUsers();
         
    }
    /**
     * This method provides the main method for the class
     * 
     * @param args Standard Java PSVM arguments
     */
    public static void main(String[] args) {
        try {
        	System.out.println("*****************************************************************************");   
        	System.out.println("User migration tool");
        	System.out.println("*****************************************************************************");
        	System.out.println();
        	UserMigrationTool ast = new UserMigrationTool();
            ast.parseArgs(args);
            ast.processCsvFile();
            ast.init();
            ast.migration();
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * DOCUMENT ME!
     * 
     * @param args DOCUMENT ME!
     */
    private void parseArgs(String[] args) {
    	if(args.length <1) {
    		 usage();
             System.exit(-1);
    	}
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-username")) {
                username = args[i].substring(args[i].indexOf("=") + 1, args[i].length());
            }
            else if (args[i].startsWith("-password")) {
                password = args[i].substring(args[i].indexOf("=") + 1, args[i].length());
                            int j = 0;
                             String encodedPwd = null;
                             if (password.length() > 0) {
                                 while (j < password.length()) {
                                     if (password.charAt(j) == '`'
                                         || password.charAt(j) == '!'
                                         || password.charAt(j) == '@'
                                         || password.charAt(j) == '#'
                                         || password.charAt(j) == '$'
                                         || password.charAt(j) == '%'
                                         || password.charAt(j) == '^'
                                         || password.charAt(j) == '&'
                                         || password.charAt(j) == '('
                                         || password.charAt(j) == ')'
                                         || password.charAt(j) == '{'
                                         || password.charAt(j) == '}'
                                         || password.charAt(j) == '['
                                         || password.charAt(j) == ']'
                                         || password.charAt(j) == '|'
                                         || password.charAt(j) == '\\'
                                         || password.charAt(j) == ';'
                                         || password.charAt(j) == '\"'
                                         || password.charAt(j) == '\''
                                         || password.charAt(j) == '<'
                                         || password.charAt(j) == '>'
                                         || password.charAt(j) == '~'
                                         || password.charAt(j) == '?'
                                         || password.charAt(j) == '/'
                                         || password.charAt(j) == ' ') {
                                         if (encodedPwd == null) {
                                             encodedPwd = "%" + DeciTohex(password.charAt(j));
                                         }
                                         else {
                                             encodedPwd += "%" + DeciTohex(password.charAt(j));
                                         }
                                     }
                                     else {
                                         if (encodedPwd == null) {
                                             encodedPwd = Character.toString(password.charAt(j));

                                         }
                                         else {
                                             encodedPwd += password.charAt(j);
                                         }
                                     }
                                     j++;
                                 }
                             }
                             password = encodedPwd;
            }
            else if (args[i].startsWith("-host")) {
                host = args[i].substring(args[i].indexOf("=") + 1, args[i].length());
            }
            else if (args[i].startsWith("-port")) {
                port = args[i].substring(args[i].indexOf("=") + 1, args[i].length());
            }
            else if (args[i].startsWith("-userFile")) {
                userFile = args[i].substring(args[i].indexOf("=") + 1, args[i].length());
            }
            else if (args[i].startsWith("-output")) {
                outputFile = args[i].substring(args[i].indexOf("=") + 1, args[i].length());
            }
            else {
                usage();
                System.exit(-1);
            }
        }
    }

    private void pressAnyKeyToContinue()
    { 
           System.out.println("Press any key to continue...");
           try
           {
               System.in.read();
           }  
           catch(Exception e)
           {}  
    }
    
    /**
     * DOCUMENT ME!
     */
    private void usage() {
        System.out.println("Migration tool (Java) parameters and options:");
        System.out.println("  -username=<value> use the specified CUCM username");
        System.out.println("  -password=<value> use the specified CUCM password ");
        System.out.println("  -host=<hostname or IP> use the specified CUCM hostname");
        System.out.println("  -port=<portnumber> use the specified portnumber to overwrite default (optional)");
        System.out.println("  -userFile=<filename> use the specified file as the source of users. Default: users.csv ");
        System.out.println("  -output=<filename> use the specified file as the destination of the AXL responses (optional)");
    }

    /* testing the SSL interface */
    public class MyTrustManager implements X509TrustManager {
        MyTrustManager() {}
        public void checkClientTrusted(X509Certificate chain[], String authType) throws CertificateException {}
        public void checkServerTrusted(X509Certificate chain[], String authType) throws CertificateException {}
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
    public static String DeciTohex(int d) {
         int rem;
         String output = "";
         String digit;
         String backwards = "";

         do {
             rem = d % 16;
             digit = DtoHex(rem);
             d = d / 16;
             output += digit;
         }
         while (d / 16 != 0);

         rem = d % 16;

         digit = DtoHex(rem);

         output = output + digit;

         for (int i = output.length() - 1; i >= 0; i--) {
             backwards += output.charAt(i);
         }

         return backwards;
     }

     public static String DtoHex(int rem) {

         String str1 = String.valueOf(rem);

         if (str1.equals("10"))
             str1 = "A";

         else if (str1.equals("11"))
             str1 = "B";

         else if (str1.equals("12"))
             str1 = "C";

         else if (str1.equals("13"))
             str1 = "D";

         else if (str1.equals("14"))
             str1 = "E";

         else if (str1.equals("15"))
             str1 = "F";
         else
        	 return str1;

         return str1;
     }
}
