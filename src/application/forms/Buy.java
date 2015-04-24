/**
The MIT License (MIT)
Copyright (c) 2015 Diego Geronimo D Onofre
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files OpenMsg, to deal
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
package application.forms;

import application.activation.ActivationFile;
import application.util.Core;

import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.util.List;
import javax.swing.JOptionPane;

import static javax.swing.JOptionPane.showMessageDialog;

public class Buy extends JFrame{

    private static final String activateServer = "activationserver.diegops.com.br";

    private static final List<Integer> portList = new ArrayList();
    
    static{
        portList.add(50010);
        portList.add(50020);
        portList.add(50030);
    }
    
    private static final String defaultEncoding = "UTF-8";
    
    private static final int TRUE = 1;
    
    private static final int FALSE = 0;
    
    private static final int ACTIVATION_REQUEST = 0;
    
    private static final int PAYMENT_URI_REQUEST = 1;
    
    private static final int TEST_REQUEST = 3;
    
    private static final int LICENSE_TYPE_REQUEST = 4;
    
    private JLabel lblApplicationKey = new JLabel("Digite a chave de licença nas caixas a baixo:");
    
    private JTextField tfdApplicationKey1 = new JTextField();
    private JTextField tfdApplicationKey2 = new JTextField();
    private JTextField tfdApplicationKey3 = new JTextField();
    private JTextField tfdApplicationKey4 = new JTextField();
    private JTextField tfdApplicationKey5 = new JTextField();
    
    private JButton btnActivate = new JButton("Ativar");
    
    private JButton btnGetKey = new JButton("Obter Chave");
    
    private static char[] numbers = new char[]{ '0',
                                                '1',
                                                '2',
                                                '3',
                                                '4',
                                                '5',
                                                '6',
                                                '7',
                                                '8',
                                                '9'};

    private static char[] letters = new char[]{ 'a',
                                                'b',
                                                'c',
                                                'd',
                                                'e',
                                                'f',
                                                'g',
                                                'h',
                                                'i',
                                                'j',
                                                'k',
                                                'l',
                                                'm',
                                                'n',
                                                'o',
                                                'p',
                                                'q',
                                                'r',
                                                's',
                                                't',
                                                'u',
                                                'v',
                                                'w',
                                                'x',
                                                'y',
                                                'z'};

    private static char[] characters = new char[36];

    static{
        for ( int i = 0; i < characters.length; i++ ){
            if ( i > 9 )
                characters[i] = String.valueOf(letters[i - 10]).toUpperCase().charAt(0);
            else
                characters[i] = numbers[i];
        }
    }   
    
    private static boolean isValidChar(char c)
    {
        for ( int i = 0; i < characters.length; i++ ){
            final char cc = characters[i];
            
            if ( cc == c )
                return true;
        }
        
        return false;
    }
    
    private static boolean isValidKey(String key)
    {
        for ( int i = 0; i < key.length(); i++ ){
            final char c = key.charAt(i);
            
            if (! isValidChar(c) )
                return false;
        }
        
        return true;
    }    
    
    public static boolean isAccessibleAndRunning(int port)
    {
        try{
            InetAddress address = InetAddress.getByName(activateServer);
            try{
                Socket socket = new Socket(address, port);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(TEST_REQUEST);
                outputStream.flush();
                InputStream inputStream = socket.getInputStream();
                int result = inputStream.read();
                socket.close();

                if ( result == TRUE ) {
                    return true;
                }
                else if ( result == FALSE ){
                    return false;
                }
                else{
                    return false;
                }
            }
            catch ( IOException e ){ 
                return false;
            }
        }
        catch (IOException e){
            return false;
        }
    }    
    
    public static int findServerPort()
    { 
        for ( Integer port : portList ){
            if ( isAccessibleAndRunning(port) )
                return port;
        }

        return -1;
    }    
    
    private boolean validateAllBoxs()
    {
        String isValidKeyMessage = " caixa de texto\n"
                                 + " possui caractere(s) inválido(s).\n"
                                 + " Note que os caracteres válidos são:\n"
                                 + " 0 até 9, A até Z (letras somente em maiúsculo).";
        
        if ( !isValidKey(tfdApplicationKey1.getText().trim())){
            JOptionPane.showMessageDialog(null, "A primeira " + isValidKeyMessage);
            tfdApplicationKey1.grabFocus();
            return false;
        }
        if ( !isValidKey(tfdApplicationKey2.getText().trim() )){
            JOptionPane.showMessageDialog(null, "A segunda " + isValidKeyMessage );
            tfdApplicationKey2.grabFocus();
            return false;
        }
        if ( !isValidKey(tfdApplicationKey3.getText().trim() ) ){
            JOptionPane.showMessageDialog(null, "A terceira " + isValidKeyMessage );
            tfdApplicationKey3.grabFocus();
            return false;
        }
        if ( !isValidKey(tfdApplicationKey4.getText().trim()) ){
            JOptionPane.showMessageDialog(null, "A quarta " + isValidKeyMessage);
            tfdApplicationKey4.grabFocus();
            return false;
        }
        if ( !isValidKey(tfdApplicationKey5.getText().trim()) ){
            JOptionPane.showMessageDialog(null, "A quinta " + isValidKeyMessage);
            tfdApplicationKey5.grabFocus();
            return false;
        }
        
        if ( tfdApplicationKey1.getText().trim().length() != 5 ){
            JOptionPane.showMessageDialog(null, "A primeira caixa de texto\n"
                                               + " não contém 5(cinco) caracteres.");
            tfdApplicationKey1.grabFocus();
            return false;
        }
        if ( tfdApplicationKey2.getText().trim().length() != 5 ){
            JOptionPane.showMessageDialog(null, "A segunda caixa de texto\n"
                                               + " não contém 5(cinco) caracteres.");
            tfdApplicationKey2.grabFocus();
            return false;
        }
        if ( tfdApplicationKey3.getText().trim().length() != 5 ){
            JOptionPane.showMessageDialog(null, "A terceira caixa de texto\n"
                                               + " não contém 5(cinco) caracteres.");
            tfdApplicationKey3.grabFocus();
            return false;
        }
        if ( tfdApplicationKey4.getText().trim().length() != 5 ){
            JOptionPane.showMessageDialog(null, "A quarta caixa de texto\n"
                                               + " não contém 5(cinco) caracteres.");
            tfdApplicationKey4.grabFocus();
            return false;
        }
        if ( tfdApplicationKey5.getText().trim().length() != 5 ){
            JOptionPane.showMessageDialog(null, "A quinta caixa de texto\n"
                                               + " não contém 5(cinco) caracteres.");
            tfdApplicationKey5.grabFocus();
            return false;
        }
        
        return true;
    }
    
    private static int isFree()
    {
        try{
            final int serverPort = findServerPort();
            
            if ( serverPort == -1 )
                return -1;
            
            InetAddress address = InetAddress.getByName(activateServer);
            Socket socket = new Socket(address, serverPort);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(LICENSE_TYPE_REQUEST);
            outputStream.flush();
            InputStream inputStream = socket.getInputStream();
            int result = inputStream.read();
            socket.close();
            return result;
        }
        catch ( Exception e ){
            return -1;
        }
    }
    
    public Buy()
    {
        final int w = 313;
        final int h = 147;
        Point p = application.forms.util.Useful.getCenterPoint(w,h);
        application.forms.util.Useful.setDefaultImageIcon( this );
        setTitle("Ativar");
        setLocation(p);
        setSize(w, h);
        setLayout(null);
        setResizable(false);
        Container contentPane = this.getContentPane();
        
        lblApplicationKey.setSize(300,20);
        lblApplicationKey.setLocation(20,10);
        
        tfdApplicationKey1.setSize(50,20);
        tfdApplicationKey1.setLocation(20,30);

        tfdApplicationKey2.setSize(50,20);
        tfdApplicationKey2.setLocation(75,30);
        
        tfdApplicationKey3.setSize(50,20);
        tfdApplicationKey3.setLocation(130,30);
        
        tfdApplicationKey4.setSize(50,20);
        tfdApplicationKey4.setLocation(185,30);
        
        tfdApplicationKey5.setSize(50,20);
        tfdApplicationKey5.setLocation(240,30);
        
        btnActivate.setSize(108, 20);
        btnActivate.setLocation(20, 60);
        btnActivate.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent me)
            {
                synchronized ( ActivationFile.fileBlocker ){
                    if (! ActivationFile.isActivated() ){
                        if ( validateAllBoxs())
                            Buy.this.activate();
                    }
                    else
                        showMessageDialog(null, "O aplicativo já está ativado!");
                }
            }
        });
        
        btnGetKey.setSize(108, 20);
        btnGetKey.setLocation(132, 60);
        btnGetKey.addMouseListener( new MouseAdapter() 
        {
            @Override
            public void mouseClicked(MouseEvent me)
            {
                synchronized ( ActivationFile.fileBlocker ){
                    if ( ActivationFile.isActivated() ){
                        showMessageDialog(null, "O aplicativo já está ativado!");
                    }
                    else{
                        final int isFree = isFree();
                        
                        if ( isFree == -1 ){
                            showMessageDialog(null, "Não foi possível se conectar ao servidor,\n"
                                                   + " por favor, tente novamente mais tarde.");
                            return;
                        }
                        else if ( isFree != TRUE ){
                            JOptionPane.showMessageDialog(null,  "Note que a chave do aplicativo " + application.Application.APPLICATION_NAME + "\n"
                                                                + "será enviada para o email informado em um dos\n"
                                                                + "diálogos de pagamento do PagSeguro a seguir.\n"                                                           
                                                                + "A chave do aplicativo será enviada logo após\n"
                                                                + "o pagamento ser processado pelo PagSeguro.\n"                                                           
                                                                + "Logo após encerrar este diálogo, será aberta\n"
                                                                + "a página do PagSeguro (ambiente seguro) para\n"
                                                                + "ser realizado o pagamento do aplicativo " + application.Application.APPLICATION_NAME + ".",
                                                          "Informação",
                                                          JOptionPane.INFORMATION_MESSAGE);
                        }

                        Buy.this.browseToBuy();
                    }
                }
            }
        });
        
        contentPane.add(lblApplicationKey);
        contentPane.add(tfdApplicationKey1);
        contentPane.add(tfdApplicationKey2);
        contentPane.add(tfdApplicationKey3);
        contentPane.add(tfdApplicationKey4);
        contentPane.add(tfdApplicationKey5);
        contentPane.add(btnActivate);
        contentPane.add(btnGetKey);
    }
    
    private void browseToBuy()
    {
        try{
            final int serverPort = findServerPort();
            
            if ( serverPort == -1 ){
                showMessageDialog(null, "Não foi possível se conectar ao servidor,\n"
                                        + " por favor, tente novamente mais tarde.");
                return;
            }
                
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            InetAddress address = InetAddress.getByName(activateServer);
            Socket socket = new Socket(address, serverPort);
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            outputStream.write(PAYMENT_URI_REQUEST);
            outputStream.flush();
            
            byte[] lengthBytes = new byte[Core.INT_SIZE];
            inputStream.read(lengthBytes);
            final int length = Core.toIntValue(lengthBytes);
            byte[] paymentURI = new byte[length];
            
            inputStream.read(paymentURI);
            String buyWebPage = new String(paymentURI, defaultEncoding);
            URI uri = new URI(buyWebPage);
            desktop.browse(uri);
        }
        catch ( URISyntaxException e ){
            throw new InternalError( "Há um problema com a sintaxe de buyWebPage" + e.toString());
        }
        catch ( java.net.ConnectException ce ){
            showMessageDialog(null, "Não foi possível se conectar ao servidor,\n"
                                    + " por favor, tente novamente mais tarde.");
        }  
        catch ( java.net.UnknownHostException hostException ){
            showMessageDialog(null, "Não foi possível se conectar ao servidor,\n"
                                   + " verifique se está conectado a internet.");
        }        
        catch ( java.net.NoRouteToHostException e ){
            showMessageDialog(null, "É necessário estar conectado a\n"
                                   + " internet para obter a chave do aplicativo.");
        }        
        catch (IOException e){
            throw new InternalError(e.toString());
        }
    }
    
    private static void activateHere()
    {
        ActivationFile.setActivated(true);
    }
    
    private String getKey()
    {
        String result = "";
        result += tfdApplicationKey1.getText()
                + tfdApplicationKey2.getText() 
                + tfdApplicationKey3.getText() 
                + tfdApplicationKey4.getText() 
                + tfdApplicationKey5.getText();
        return result;
    }
    
    private void activate()
    {
        try{
            final int serverPort = findServerPort();
            
            if ( serverPort == -1 ){
                showMessageDialog(null, "Não foi possível se conectar ao servidor,\n"
                                        + " por favor, tente novamente mais tarde.");
                return;
            }
            
            InetAddress address = InetAddress.getByName( activateServer);
            Socket socket = new Socket(address, serverPort);
            byte[] key = getKey().trim().getBytes(defaultEncoding);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(ACTIVATION_REQUEST);
            outputStream.write(key);
            outputStream.flush();
            InputStream inputStream = socket.getInputStream();
            final int firstValue = inputStream.read();
            
            if ( firstValue == TRUE )
            {   
                activateHere();
                
                //A linha abaixo está notificando o servidor 
                //para que o mesmo ative efetivamente no servidor 
                outputStream.write(TRUE);
                outputStream.flush();            
                this.dispose();
                showMessageDialog(null, "O aplicativo foi ativado com sucesso!\n"
                                      + "Agora será possivel executar este aplicativo\n"
                                      + "quantas vezes forem necessárias\n"
                                      + "e sem  limite de tempo por execução.\n"
                                      + "Obrigado por ativar o " + application.Application.APPLICATION_NAME + ".\n", 
                                 "Informação", 
                                 JOptionPane.INFORMATION_MESSAGE);
            }
            else if ( firstValue == FALSE )
            {
                outputStream.write(FALSE);
                outputStream.flush();
                showMessageDialog(null, "A chave digitada não é válida.\n"
                                       + "Por favor, verifique!");
            }
            else
                showMessageDialog(null, "Ocorreu um problema ao ativar a aplicação,\n"
                                        + " por favor tente novamente.");
            
            inputStream.close();
            outputStream.close();
            socket.close();
        }
        catch ( java.net.ConnectException ce ){
            showMessageDialog(null, "Não foi possível se conectar ao servidor,\n"
                                    + " por favor, tente novamente mais tarde.");
        }
        catch ( java.net.UnknownHostException hostException ){
            showMessageDialog(null, "Não foi possível se conectar ao servidor,\n"
                                   + " verifique se está conectado a internet.");
        }
        catch ( java.net.NoRouteToHostException e ){
            showMessageDialog(null, "É necessário estar conectado a\n"
                                   + " internet para ativar este aplicativo.");
        }
        catch ( Exception e ){
            throw new InternalError(e.toString());
        }
    }
}
