
/**
The MIT License (MIT)
Copyright (c) 2015 Diego Geronimo D Onofre
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files OpenMsg Server, to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package application;

import application.forms.Server;
import application.net.core.Packet;
import application.net.core.Request;
import application.requests.FriendRequest;
import application.requests.InvitationRequest;
import application.requests.MessageRequest;
import application.requests.NetworkRequest;
import application.requests.RegisterRequest;
import application.requests.UpdateRequest;
import application.net.core.IdentifierManager;
import application.requests.ApplicationRequest;
import application.xml.ApplicationXml;
import application.activation.ActivationFile;

import com.jtattoo.plaf.acryl.AcrylDefaultTheme;
import com.jtattoo.plaf.acryl.AcrylLookAndFeel;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import static javax.swing.JOptionPane.showMessageDialog;

/**
*
* @author   Diego Geronimo Onofre
* @channel  https://www.youtube.com/user/cursostd
* @facebook https://www.facebook.com/diegogeronimoonofre
* @Github   https://github.com/DiegoGeronimoOnofre
*/

public class Application 
{       
        /* A variável mainPath deve ser 
         * definida antes de APPLICATION_VERSION */
    
        private static final String mainPath = System.getProperty( "user.dir" );
    
        public static final int BEGIN_PORT = 30000;
        
        public static final int END_PORT = 30010;
        
        public static final String APPLICATION_NAME = "OpenMsg Server";
        
        public static final String APPLICATION_VERSION = ApplicationXml.getApplicationVersion();
        
        public static final String APPLICATION_DEVELOPER = "Diego Geronimo D Onofre";
        
        public static final String CONTACT_EMAIL = "diegogeronimoonofre@outlook.com";
        
        public static String getMainPath()
        {
                return mainPath;
        }
        
        public static void addRequests() throws Exception
        {
                Request.addRequestClass( MessageRequest.class );                   
                Request.addRequestClass( NetworkRequest.class );                               
                Request.addRequestClass( UpdateRequest.class );                               
                Request.addRequestClass( InvitationRequest.class );                               
                Request.addRequestClass( FriendRequest.class );                               
                Request.addRequestClass( RegisterRequest.class );   
                Request.addRequestClass( ApplicationRequest.class );
        }           
        
        public static void main( String[] args )
        {   
                if ( ! IdentifierManager.isFirstApplicationInstance() ){
                        showMessageDialog(null,"Não é possível iniciar outra "
                                              + "instância de aplicativo " + APPLICATION_NAME + "!");
                        return;
                }
                
                Packet.configIdentifierManager(IdentifierManager.SERVER_APPLICATION);
                
                try{
                        LookAndFeel lf = new AcrylLookAndFeel();
                        AcrylLookAndFeel.setTheme( new AcrylDefaultTheme() );
                        UIManager.setLookAndFeel( lf );
                        
                }
                catch ( Exception e ){ 
                        throw new InternalError( e.toString() );
                }                
                
                try {                                
                        addRequests();
                        int activationResult = ActivationFile.check();
        
                        if ( activationResult != ActivationFile.IS_ACTIVATED 
                          && activationResult != ActivationFile.IN_ANALYSIS)
                            return;
                        
                        Server server = new Server();      
                        server.init();
                }
                catch ( Exception e ){
                        showMessageDialog( null, e.getMessage() );
                }
        }
}
