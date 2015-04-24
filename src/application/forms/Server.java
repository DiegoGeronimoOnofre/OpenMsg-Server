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

import application.Application;
import application.InvitationResponse;
import application.db.Database;
import application.forms.util.UserStatus;
import application.net.Manager;
import application.net.Network;
import application.net.core.Packet;
import application.net.core.PacketSocket;
import application.net.core.Request;
import application.objects.Status;
import application.requests.NetworkRequest;
import application.requests.UpdateRequest;
import application.util.Convertible;
import application.forms.util.Users;
import application.forms.util.Useful;
import application.net.core.InvalidPacketFormatException;
import application.net.core.NetworkException;
import application.net.core.RequestSocket;
import application.objects.Bool;
import application.objects.Invitation;
import application.objects.NickName;
import application.objects.St;
import application.objects.XStatus;
import application.objects.LogonResult;
import application.objects.Friends;
import application.objects.InvitationResult;
import application.objects.Register;
import application.objects.Logon;
import application.objects.UserAndFriend;
import application.requests.ApplicationRequest;
import application.requests.FriendRequest;
import application.requests.InvitationRequest;
import application.requests.RegisterRequest;
import application.util.Blocker;
import application.util.Core;
import application.util.Executable;
import application.util.ObjectList;
import application.activation.ActivationFile;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.InetAddress;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import static javax.swing.JOptionPane.showMessageDialog;

public class Server
{        
        private static final String path = System.getProperty( "user.dir" );
        
        private static final String pathDB = path + File.separatorChar + "DATABASE.FDB";        
        
        public static String getPathDB()
        {
                return pathDB;
        }   
        
        public static final long DEFAULT_MAX_TIME = 30000l;
        
        public static final long REGISTER_MAX_TIME = 40000l;
   
        private PacketSocket ps;    
        
        private Connection con;
        
        private UsersGUI usersGUI = new UsersGUI();
        
        private Users users = usersGUI.getDefaultUsers();
        
        private Network network = new Network();        
        
        private MenuItem mniRun = new MenuItem( "Executar" );
        
        private MenuItem mniShowUsers = new MenuItem( "Usuários Conectados" );
        
        private MenuItem mniManual = new MenuItem("Manual");
        
        private MenuItem mniAbout = new MenuItem("Sobre");
        
        private MenuItem mniActivate = new MenuItem("Ativar");
        
        private MenuItem mniExit = new MenuItem( "Sair" );
        
        public static final int STOPPED = 0;
        
        public static final int RUNNING = 1;
        
        private int state = STOPPED;
    
        private final Object executionLock = new Object();    
        
        /*Valor em horas*/
        
        private final long defaultTerminationInterval = 2;
        
        public Server()
        {                                 
                usersGUI.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
                boolean isActivated = ActivationFile.isActivated();
                
                if ( !isActivated ){
                    new Thread()
                    {
                        @Override
                        public void run()
                        {
                            try{
                                new Thread()
                                {
                                    @Override     
                                    public void run()
                                    {
                                        try{
                                            Thread.sleep(10000l);
                                        }
                                        catch ( Exception e ){
                                            throw new InternalError(e.toString());
                                        }
                                        java.awt.Toolkit.getDefaultToolkit().beep();
                                        JOptionPane.showMessageDialog(null, 
                                                                      "Note que o aplicativo " + Application.APPLICATION_NAME + "\n"
                                                                          + "é encerrado depois de " + defaultTerminationInterval + " horas em\n"
                                                                          + "execução quando está no período de testes.", 
                                                                      "Informação", 
                                                                      JOptionPane.INFORMATION_MESSAGE);
                                    }
                                
                                }.start();
                                        
                                long time = defaultTerminationInterval * 60l * 60l * 1000l;
                                Thread.sleep(time);
                                long infoInterval = 5;
                                
                                if (! ActivationFile.isActivated() ){
                                    java.awt.Toolkit.getDefaultToolkit().beep();
                                    trayIcon.displayMessage("", 
                                                            "O " + Application.APPLICATION_NAME + " será "
                                                                + "encerrado em " + infoInterval + " minutos.\n"
                                                                + "Note que este comportamento é realizado somente em "
                                                                + "aplicativos " + Application.APPLICATION_NAME + " que ainda não foram ativados.", 
                                                           TrayIcon.MessageType.WARNING);
                                }
                                Thread.sleep(infoInterval * 60l * 1000l);
                                
                                if (! ActivationFile.isActivated() )
                                    Server.this.finalizeServer();
                            }
                            catch ( Exception e ){
                                throw new InternalError(e.toString());
                            }
                        }
                    }.start();
                }
        }    
        
        private final Object initSync = new Object();
        
        private boolean initialized = false;

        private final SystemTray systemTray; 
        
        private final TrayIcon trayIcon;
        
        {
                if ( ! SystemTray.isSupported() )
                {
                        showMessageDialog( null,  Application.APPLICATION_NAME 
                                                  + " não pode ser inicializado \n"
                                                  + "no sistema operacional em questão!" );
                        System.exit( 0 );
                }        
                
                systemTray = SystemTray.getSystemTray();      
                final char sep = File.separatorChar;
                String way = path + sep + "imgs" + sep + "MainIcon.jpg";
                Image icon = new ImageIcon( way ).getImage();         
                trayIcon = new TrayIcon( icon );       
        }
        
        private void runServer()
        {
                synchronized ( executionLock )
                {
                        if ( state == STOPPED )
                        {
                                Server.this.run();

                                if ( state == RUNNING ) 
                                {
                                        Server.this.mniRun.setLabel( "Parar" );
                                        trayIcon.displayMessage( "",  Application.APPLICATION_NAME 
                                                                      + " está em execução!", 
                                                                      TrayIcon.MessageType.INFO );
                                }
                        }
                        else if ( state == RUNNING )
                        {
                                Server.this.close();

                                if ( state == STOPPED )
                                {
                                        Server.this.mniRun.setLabel( "Executar" );
                                        trayIcon.displayMessage( "",  Application.APPLICATION_NAME 
                                                                     + " está parado!", 
                                                                     TrayIcon.MessageType.INFO );
                                }
                        }
                }
        }
        
        private void initialize()
        {
                mniRun.addActionListener( new ActionListener()
                {
                        @Override
                        public void actionPerformed( ActionEvent event )
                        {
                            Server.this.runServer();
                        }
                } );

                mniShowUsers.addActionListener( new ActionListener()
                {
                        @Override
                        public void actionPerformed( ActionEvent event )
                        {                                             
                                synchronized ( executionLock )
                                {
                                        if ( ! Server.this.isRunning() )
                                        {
                                                showMessageDialog( null,  
                                                                    Application.APPLICATION_NAME 
                                                                    + " não está em execução!" );
                                        }
                                        else if ( ! usersGUI.isShowing()  )
                                        {
                                                usersGUI.setVisible( true );
                                                usersGUI.repaint();
                                        }
                                }
                        }
                } );  
                
                mniManual.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent me)
                    {
                        String manualPath = application.Application.getMainPath() + File.separatorChar + "Manual.pdf";
                        File file = new File(manualPath);
                        
                        try{
                            java.awt.Desktop.getDesktop().open(file);
                        }
                        catch (Exception e) {
                            JOptionPane.showMessageDialog(null, 
                                                            " Infelizmente um problema ocorreu ao abrir o manual.\n"
                                                          + " Este erro pode ter sido gerado, porque\n"
                                                          + " neste computador não há nenhum leitor pdf.\n"
                                                          + " Por favor, instale um leitor pdf e vincule a extenção pdf\n"
                                                          + " com este leitor e tente abrir o manual novamente.");
                        }
                    }
                });
                
                mniAbout.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent me)
                    {
                        javax.swing.JOptionPane.showMessageDialog(null,
                                                                 "\nVersão do " + Application.APPLICATION_NAME + ": "
                                                               + Application.APPLICATION_VERSION + "\n"
                                                               + "Desenvolvido por: " + Application.APPLICATION_DEVELOPER + "          \n"
                                                               + "Email: " + Application.CONTACT_EMAIL + "\n",
                                                                 "Sobre " + Application.APPLICATION_NAME,
                                                                 javax.swing.JOptionPane.INFORMATION_MESSAGE);                        
                    }
                });
                
                mniActivate.addActionListener( new ActionListener()
                {
                    @Override
                    public void actionPerformed( ActionEvent event )
                    {
                        Buy buy = new Buy();
                        buy.setDefaultCloseOperation(Buy.DISPOSE_ON_CLOSE);
                        buy.setVisible(true);
                    }
                });
                
                mniExit.addActionListener( new ActionListener()
                {
                        @Override
                        public void actionPerformed( ActionEvent event )
                        {
                                Server.this.finalizeServer();
                        }
                }
                );
                
                PopupMenu popup = new PopupMenu();
                popup.add( mniRun );
                popup.add( mniShowUsers );
                popup.add( mniManual );
                popup.add( mniAbout );
                
                if ( ! ActivationFile.isActivated() )
                    popup.add(mniActivate);
                    
                popup.add( mniExit );
                trayIcon.setPopupMenu( popup );
                
                try{
                        systemTray.add( trayIcon );
                }
                catch ( Exception e ) {
                        throw new InternalError( e.toString() );
                }
        }
        
        private void finalizeServer()
        {
                synchronized ( executionLock )
                {
                        if ( Server.this.isRunning() )
                        {
                                Server.this.close();
                                trayIcon.displayMessage( "", 
                                                          Application.APPLICATION_NAME 
                                                         + " está parado!\n O aplicativo será encerrado!", 
                                                         TrayIcon.MessageType.INFO );

                                try
                                {
                                        Thread.sleep(3000l);
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError(e.toString());
                                }
                        }

                        systemTray.remove( trayIcon );
                        System.exit( 0 );
                }        
        }
        
        public void init()
        {
                synchronized ( initSync )
                {
                        if ( ! initialized )
                        {
                                initialize();
                                Server.this.runServer();
                                initialized = true;
                        }
                        else
                                throw new RuntimeException( "server já foi inicializado!" );
                }
        }
        
        public boolean isRunning()
        {
                synchronized ( executionLock )
                {
                      return state == RUNNING;
                }
        }
        
        private final ObjectList<ServerThread> threadList = new ObjectList<ServerThread>();

        private final Object serverThreadLock = new Object();        

        private final Object joinAndRemoveAllLock = new Object();
        
        private boolean isRemovingThreads = false;
        
        private void joinAndRemoveAll()
        {            
                synchronized ( joinAndRemoveAllLock )
                {
                        isRemovingThreads = true;
                }
            
                synchronized ( serverThreadLock )
                {
                        while ( ! threadList.isEmpty() )
                        {
                                try
                                {
                                        Thread t = threadList.remove();
                                        t.join();
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }
                        }
                }
                
                synchronized ( joinAndRemoveAllLock )
                {
                        isRemovingThreads = false;
                }
        }        

        private final Object identifierLock = new Object();
        
        private long nextIdentifier = 0;
        
        private class ServerThread extends Thread
        {       
                private final long identifier;
                
                public ServerThread()
                {
                        synchronized ( identifierLock )
                        {
                                nextIdentifier++;
                                identifier = nextIdentifier;   
                        }
                }
                
                public long getIdentifier()
                {
                        return identifier;
                }
                
                @Override
                public void start()
                {   
                        synchronized ( joinAndRemoveAllLock )
                        {
                                if ( ! isRemovingThreads )
                                {
                                        synchronized ( serverThreadLock )
                                        {
                                                threadList.add( this );
                                        }

                                        super.start();

                                        new Thread()
                                        {
                                                @Override
                                                public void run()
                                                {
                                                        ServerThread.this.joinAndRemove();
                                                }
                                        }.start();
                                }
                        }
                }
                
                public void joinAndRemove()
                {
                        try
                        {
                                super.join();
                                
                                synchronized ( serverThreadLock )
                                {
                                        if ( ! threadList.isEmpty() )
                                        {
                                                threadList.first();

                                                do
                                                {
                                                        ServerThread st = threadList.get();                                               

                                                        if ( st.getIdentifier() == this.getIdentifier() )
                                                        {
                                                                threadList.remove();
                                                                break;
                                                        }
                                                } while ( threadList.next() );
                                        }
                                }
                        }
                        catch ( Exception e )
                        {
                                throw new InternalError( e.toString() );
                        }
                }
        }
        
        private interface DownloadFunction
        {
                public void run( byte[] information );
        }        
        
        private class DownloadThread extends ServerThread
        {
                private final Network network;
                
                private final Packet p;
                
                private final DownloadFunction function;
                
                public DownloadThread( Network network, 
                                       Packet p, 
                                       DownloadFunction function )
                {
                        this.network = network;
                        this.p = p;
                        this.function = function;
                }
                
                @Override
                public void run()
                {
                        try
                        {
                                byte[] information = network.download( p );
                                
                                if ( information != null )
                                        function.run( information );
                        }
                        catch ( Exception e )
                        {
                                throw new InternalError( e.getMessage() );
                        }                        
                }
        }
        
        private class SendObjectThread extends ServerThread
        {
                private final Network network;
                
                private final Packet p;
                
                private final application.net.Run run;
                
                public SendObjectThread( Network network, 
                                         Packet p, 
                                         application.net.Run run )
                {
                        this.network = network;
                        this.p = p;
                        this.run = run;
                }
                
                @Override
                public void run()
                {       
                        try
                        {
                                network.sendObject( p, run );                       
                        }
                        catch ( Exception e )
                        {
                                throw new InternalError( e.toString() );
                        }
                }
         }  
        
        private static boolean updateProps( St st, 
                                             InetAddress address, 
                                             final int port )
        {                 
                 if ( address == null )
                         throw new IllegalArgumentException( "address é null" );
                 
                 if ( port < 0 || port > 0xFFFF )
                         throw new IllegalArgumentException( "port não armazena um número de porta válida" );
                
                 try
                 {
                         Packet p = Packet.forUpload( address, 
                                                      port, 
                                                      new UpdateRequest( UpdateRequest.STATUS_UPDATE_REQUEST ) );

                         Network net = new Network();
                         net.getUploadManager().add( p );
                         return net.upload( p,
                                            st.toBytes() );  
                 }
                 catch ( Exception e )
                 {
                         throw new InternalError( e.toString() );
                 }
        }
          
        private final Blocker notifyListBlocker = new Blocker(); 
        
        {
            notifyListBlocker.init();
        }
         
        private final List<NotifyAllFriendsThread> notifyList = new ArrayList<NotifyAllFriendsThread>()
        {
                @Override
                public int indexOf( Object obj )
                {
                        if ( obj == null )
                                throw new InternalError("obj é null ArrayList.indexOf()@Override");
                        
                        for ( int i = 0; i < size(); i++ )
                        {
                                Object o = get( i );
                                
                                if ( o == obj )
                                        return i;
                        }
                        
                        return -1;
                }
        };          
        
        private class NotifyAllFriendsThread extends ServerThread
        {
                private final St status;
                
                private boolean stopped = false;
                
                public NotifyAllFriendsThread( St status )
                {
                        if ( status == null )
                                throw new IllegalArgumentException( "status é null" );
                        
                        this.status = status;
                }
                 
                public St getStatus()
                {
                        return status;
                }
                
                public boolean isStopped()
                {
                        return stopped;
                }
                
                /*Este método deve ser invocado 
                 * somente em blocos sincronizados
                 */
                
                public void stopNotification()
                {                     
                         if ( stopped )
                                 return;                                    

                         stopped = true;
                         final int index = notifyList.indexOf( this );

                         if ( index == -1 )
                                 throw new InternalError( "index == -1" );

                         notifyList.remove( index );                                     
                }
                
                private void update()
                {
                        try
                        {
                                 St stt = ( St ) Convertible.bytesToObject( status.toBytes() );
                                 Friends fs = Database.getFriends( con, stt.getNickName() );
                                 List<String> friendList = fs.getFriends();
                                 final int size = friendList.size();
                                 
                                 for ( int i = 0; i < size; i++)
                                 {
                                         if ( isStopped() )
                                                 return;
                                         
                                         final String friend = friendList.get( i );
                                         UserStatus us = users.getUserStatus( friend );

                                         if ( us == null )
                                                 continue;

                                         stt.setRecipient( friend );
                                         
                                         final Object lock = users.getDefaultUsersSync();
                                         final InetAddress address;
                                         final int port;
                                         
                                         synchronized ( lock )
                                         {
                                                 address = us.getAddress();
                                                 port = us.getPort();
                                         }
                                         
                                         updateProps( stt, 
                                                      address, 
                                                      port );

                                 }
                        }
                        catch ( Exception e )
                        {
                                throw new InternalError( e.toString() );
                        }

                        /*Este fluxo é criado 
                         * para evitar que o A espere o B
                         * e o B espere o A.
                         */
                        
                        class XServerThread extends ServerThread
                        {
                                @Override
                                public void run()
                                {
                                        class Function extends Executable
                                        {
                                                @Override
                                                public void run()
                                                {
                                                       stopNotification();
                                                }
                                        }
                                        
                                        Function function = new Function();
                                        notifyListBlocker.execute(function);
                                }
                        }
                        
                        XServerThread th = new XServerThread();
                        th.setName("XServerThread.stopNotification");
                        th.start();
                 }                         

                 @Override
                 public void run()
                 {
                         update();
                 }
        }

        private void notifyAllFriends( final St status )
        {
                class Function extends Executable
                {
                        @Override
                        public void run()
                        {
                                final int size = notifyList.size();

                                for ( int i = 0; i < size; i++ )
                                {
                                        final NotifyAllFriendsThread thread = notifyList.get( i );
                                        String nickName = thread.getStatus().getNickName().trim();

                                        if ( nickName.equalsIgnoreCase( status.getNickName().trim() ) )
                                        {
                                                thread.stopNotification();

                                                try {
                                                        thread.join();
                                                        break;
                                                }
                                                catch ( Exception e ){
                                                        throw new InternalError( e.toString() );
                                                }
                                        }
                                }

                                NotifyAllFriendsThread th = new NotifyAllFriendsThread( status );
                                notifyList.add( th );    
                                th.setName("notifyAllFriends(" + status.getNickName() + ")");
                                th.start();
                        }
                }

                Function function = new Function();
                notifyListBlocker.execute(function);              
        }  
        
        private void updateUsersGUI()
        {
                if ( usersGUI.isShowing() )
                {
                        usersGUI.validate();
                        usersGUI.repaint();
                }    
                else
                        usersGUI.validate();
        }
        
        private boolean isConnected( UserStatus us )
        {                
                try
                {
                        final Object lock = users.getDefaultUsersSync();
                        final InetAddress address;
                        final int port;

                        synchronized ( lock )
                        {
                                address = us.getAddress();
                                port = us.getPort();
                        }                        
                        
                        Packet p = Packet.createSmartRequest( address, 
                                                              port, 
                                                              new UpdateRequest( UpdateRequest.CONNECTED_USER_REQUEST ),
                                                              us.getNickName().getBytes() );

                        RequestSocket rs = Network.createDefaultRequestSocket();
                        Packet packet = rs.request( p );
                        
                        if ( packet != null )
                        {
                                Request request  = packet.getRequest();
                                
                                if ( ! ( request instanceof UpdateRequest ) )
                                        throw new InternalError( "! ( packet.getRequest() instanceof UpdateRequest )" );
                                
                                if ( request.getRequestID() != UpdateRequest.CONNECTED_USER_REQUEST )
                                        throw new InternalError( "request.getRequestID() != UpdateRequest.CONNECTED_USER_REQUEST" );
                                        
                                byte[] data = packet.getData();
                                
                                if ( data.length != 1 )
                                        throw new InternalError( "data.length != 1" );
                                
                                return Core.toBooleanValue( data[0] );                                
                        }
                        else
                                return false;
                        
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        }

        private static void finalizeClient( String nickName, 
                                            InetAddress address, 
                                            final int port )
        {         
                if ( nickName == null )
                        throw new IllegalArgumentException( "nickName é null" );
                        
                if ( address == null )
                        throw new IllegalArgumentException( "address é null" );        
                
                if ( port < 0 || port > 0xFFFF )
                        throw new IllegalArgumentException( "port não armazena um número de porta válida" );                    
                
                try
                {
                        Packet p = Packet.createSmartRequest( address, 
                                                              port, 
                                                              new UpdateRequest( UpdateRequest.DISCONNECT_REQUEST ),
                                                              nickName.getBytes() );

                        RequestSocket rs = Network.createDefaultRequestSocket();
                        Packet packet = rs.request( p );
                        
                        if ( packet != null )
                        {
                                if ( packet.getPacketType() != Packet.SMART_RESULT_TYPE )
                                        throw new InternalError( "packet.getPacketType != Packet.SMART_RESULT_TYPE" );
                                
                                Request request  = packet.getRequest();
                                
                                if ( ! ( request instanceof UpdateRequest ) )
                                        throw new InternalError( "! ( packet.getRequest() instanceof UpdateRequest )" );
                                
                                if ( request.getRequestID() != UpdateRequest.DISCONNECT_REQUEST )
                                        throw new InternalError( "request.getRequestID() != UpdateRequest.DISCONNECT_REQUEST" );                         
                        }
                        
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        }

        
        private void inviteIfConnected( Invitation invitation )
        {
                try
                {
                        String user = invitation.getUser();
                        UserStatus us = users.getUserStatus( user );

                        if ( us != null )
                        {      
                                final Object lock = users.getDefaultUsersSync();
                                final InetAddress address;
                                final int port;

                                synchronized ( lock )
                                {
                                        address = us.getAddress();
                                        port = us.getPort();
                                }                                
                                
                                Network net = new Network();                                                                                    
                                Packet packet = Packet.forUpload( address,
                                                                  port,
                                                                  new InvitationRequest( InvitationRequest.INVITATION_REQUEST ));

                                net.getUploadManager().add( packet );
                                net.upload( packet, invitation.toBytes() );
                        }
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        }
        
        private void checkInvitations( final String nickName )
        {
                new ServerThread()
                {
                        @Override
                        public void run()
                        {
                                try{
                                        List<String> friendList = Database.getInvitations( con, nickName );
                                        final int size = friendList.size();
                                        
                                        for ( int i = 0; i < size; i ++ )
                                        {
                                                final String friend = friendList.get( i );
                                                final Invitation invitation = new Invitation();
                                                invitation.setUser( nickName );
                                                invitation.setFriend( friend );

                                                new ServerThread()
                                                {
                                                        @Override
                                                        public void run()
                                                        {
                                                                inviteIfConnected( invitation );
                                                        }
                                                }.start();
                                        }

                                }
                                catch ( Exception e ) {
                                        throw new InternalError( e.toString() );
                                }
                        }
                }.start();
        }        

        private void checkDeclined( final String nickName )
        {
                new ServerThread()
                {
                        @Override
                        public void run()
                        {
                                try
                                {
                                        List<String> friendList = Database.getDeclineds( con, nickName );
                                        final int size = friendList.size();
                                        UserStatus us = users.getUserStatus( nickName );
                                        
                                        if ( us != null )
                                        {
                                                for ( int i = 0; i < size; i ++ )
                                                {
                                                        final String friend = friendList.get( i );
                                                        final Invitation invitation = new Invitation();
                                                        invitation.setUser( nickName );
                                                        invitation.setFriend( friend );
                                                        invitation.setAnswer( Invitation.DECLINED );
                                                        Network net = new Network();
                                                        final Object lock = users.getDefaultUsersSync();
                                                        final InetAddress address;
                                                        final int port;

                                                        synchronized ( lock )
                                                        {
                                                                address = us.getAddress();
                                                                port = us.getPort();
                                                        }                                                        
                                                        
                                                        Packet packet = Packet.forUpload( address,
                                                                                          port,
                                                                                          new InvitationRequest( InvitationRequest.INVITATION_ANSWER_REQUEST ) );

                                                        net.getUploadManager().add( packet );
                                                        boolean result = net.upload( packet, invitation.toBytes() );  

                                                        if ( result == true )
                                                                Database.deleteDeclined( con, friend, nickName );
                                                }
                                        }

                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }
                        }
                }.start();
        }      
        
        private void addUS( UserStatus us )
        {
                if ( us == null )
                        throw new InternalError( "us == null" );
                
                final Object lock = users.getDefaultUsersSync();
                
                synchronized ( lock )
                {
                        String nickName = us.getNickName();
                        UserStatus userStatus = users.getUserStatus( nickName );
                        
                        if ( userStatus == null )
                                users.addUserStatus( us );
                        else
                        {
                                InetAddress address = us.getAddress();
                                
                                if ( address != null )
                                        userStatus.setAddress( address );
                                
                                final int port = us.getPort();
                                
                                if ( port != -1 )
                                        userStatus.setPort( us.getPort() );
                                
                                userStatus.setStatus( us.getStatus() );
                        }
                }
        }
        
        private class MainThread extends ServerThread
        { 
                private final Manager downloadManager = network.getDownloadManager();  
                private final Manager sendObjectManager = network.getSendObjectManager();                
                
                @Override
                public void run()
                {                
                        System.out.println("mainThread foi iniciada.");
                    
                        while ( state == RUNNING )
                        {
                                try
                                {
                                        final Packet p = ps.receive();
                                        final Request request = p.getRequest();                                        
                                        
                                        if ( p.getPacketType() == Packet.SMART_REQUEST_TYPE ){
                                            if ( request instanceof UpdateRequest ){
                                                if ( request.getRequestID() == UpdateRequest.SERVER_CLOSING_REQUEST ){
                                                    System.out.println("mainThread terminou.");
                                                    return;
                                                }
                                            }
                                        }
                                            
                                        if ( p.getPacketType() == Packet.SMART_REQUEST_TYPE )
                                        {           
                                                if (request instanceof ApplicationRequest)
                                                {
                                                    if ( request.getRequestID() == ApplicationRequest.APPLICATION_VERSION_REQUEST )
                                                    {
                                                        new ServerThread()
                                                        {
                                                            @Override
                                                            public void run()
                                                            {
                                                                try
                                                                {
                                                                    Packet packet = Packet.createSmartResult(p.getIdentifier(), 
                                                                                                             p.getAddress(), 
                                                                                                             p.getPort(), 
                                                                                                             p.getRequest(),
                                                                                                             Application.APPLICATION_VERSION.trim().getBytes());
                                                                    ps.send(packet);
                                                                }
                                                                catch (Exception e)
                                                                {
                                                                    throw new InternalError(e.toString());
                                                                }
                                                            }
                                                        }.start();
                                                    }
                                                }
                                                else if ( request instanceof RegisterRequest )
                                                {
                                                        if ( request.getRequestID() == RegisterRequest.REGISTER_MAX_TIME_REQUEST )
                                                        {
                                                                new ServerThread()
                                                                {
                                                                        @Override
                                                                        public void run()
                                                                        {
                                                                                try
                                                                                {
                                                                                        Packet packet = Packet.createSmartResult( p.getIdentifier(),
                                                                                                                                  p.getAddress(),
                                                                                                                                  p.getPort(),
                                                                                                                                  p.getRequest(),
                                                                                                                                  Core.toLongBytes( REGISTER_MAX_TIME ));
                                                                                        ps.send( packet );
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }.start();
                                                        }
                                                }
                                                else if ( request instanceof UpdateRequest )
                                                {
                                                        if ( request.getRequestID() == UpdateRequest.SERVER_RUNNING_REQUEST )
                                                        {
                                                                new ServerThread()
                                                                {
                                                                        @Override
                                                                        public void run()
                                                                        {
                                                                                try
                                                                                {
                                                                                        Packet packet = Packet.createSmartResult( p.getIdentifier(),
                                                                                                                                  p.getAddress(),
                                                                                                                                  p.getPort(),
                                                                                                                                  p.getRequest(),
                                                                                                                                  new byte[]{} );

                                                                                        ps.send( packet );                                                                                        
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }.start();
                                                        }   
                                                }
                                                else if ( request instanceof NetworkRequest )
                                                {
                                                        if ( request.getRequestID() == NetworkRequest.MAX_TIME_REQUEST )
                                                        {
                                                                new ServerThread()
                                                                {
                                                                        @Override
                                                                        public void run()
                                                                        {
                                                                                try {
                                                                                        Packet packet = Packet.createSmartResult( p.getIdentifier(),
                                                                                                                                  p.getAddress(),
                                                                                                                                  p.getPort(),
                                                                                                                                  p.getRequest(),
                                                                                                                                  Core.toLongBytes( DEFAULT_MAX_TIME ) );
                                                                                        ps.send( packet );
                                                                                }
                                                                                catch ( Exception e ) {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }.start();                                                        
                                                        }
                                                        else if ( request.getRequestID() == NetworkRequest.PORT_REQUEST )
                                                        {
                                                                new ServerThread()
                                                                {
                                                                        @Override
                                                                        public void run()
                                                                        {
                                                                                try
                                                                                {
                                                                                        Packet packet = Packet.createSmartResult( p.getIdentifier(),
                                                                                                                                  p.getAddress(),
                                                                                                                                  p.getPort(),
                                                                                                                                  p.getRequest(),
                                                                                                                                  new byte[]{} );

                                                                                        ps.send( packet );
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }                                                                                
                                                                        }
                                                                }.start();
                                                        }
                                                }
                                        }   
                                        else if ( p.getPacketType() == Packet.SMART_RESULT_TYPE )
                                        {          
                                                if ( request instanceof RegisterRequest )
                                                {
                                                        if ( request.getRequestID() == RegisterRequest.REGISTER_REQUEST )
                                                        {
                                                                class Function implements application.net.Run
                                                                {                                                                    
                                                                        @Override
                                                                        public Convertible run( Convertible obj )
                                                                        {
                                                                                try
                                                                                {
                                                                                        Register register = ( Register ) obj;
                                                                                        return Database.register( con, register );
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }
                                                                
                                                                if ( sendObjectManager.add( p ) )
                                                                        new SendObjectThread( network, p, new Function() ).start();        
                                                        }
                                                }
                                                else if ( request instanceof FriendRequest )
                                                {
                                                        if ( request.getRequestID() == FriendRequest.IS_FRIEND_REQUEST )
                                                        {
                                                                class Function implements application.net.Run
                                                                {
                                                                        @Override
                                                                        public Convertible run( Convertible obj )
                                                                        {
                                                                                try
                                                                                {
                                                                                        Invitation invitation = ( Invitation ) obj;
                                                                                        Bool bool = new Bool();
                                                                                        boolean result = Database.isFriend( con, 
                                                                                                                            invitation.getUser(), 
                                                                                                                            invitation.getFriend() );
                                                                                        bool.setBoolean( result );
                                                                                        return bool;
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }
                                                                
   
                                                                if ( sendObjectManager.add( p ) )
                                                                        new SendObjectThread( network, p, new Function() ).start();                                                                
                                                        }
                                                        else if ( request.getRequestID() == FriendRequest.FRIENDS_REQUEST )
                                                        {
                                                                class Function implements application.net.Run
                                                                {
                                                                        @Override
                                                                        public Convertible run( Convertible obj )
                                                                        {
                                                                                NickName nn = ( NickName ) obj;
                                                                                return Database.getFriends( con, nn.getNickName() );
                                                                        }
                                                                }
                                                                
   
                                                                if ( sendObjectManager.add( p ) )
                                                                        new SendObjectThread( network, p, new Function() ).start();                                                                
                                                        }
                                                        else if ( request.getRequestID() == FriendRequest.FRIEND_DELETE_REQUEST )
                                                        {
                                                                class Function implements DownloadFunction
                                                                {
                                                                        @Override
                                                                        public void run( byte[] information )
                                                                        {
                                                                                try
                                                                                {
                                                                                        UserAndFriend userAndFriend = ( UserAndFriend ) Convertible.bytesToObject( information );
                                                                                        String user = userAndFriend.getUser();
                                                                                        String friend = userAndFriend.getFriend();
                                                                                        Database.deleteFriend( con, user, friend );
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }
                                                                
                                                                if ( downloadManager.add( p ) )
                                                                        new DownloadThread( network, p, new Function() ).start();                                                                
                                                        }
                                                }
                                                else if ( request instanceof InvitationRequest )
                                                {
                                                        if ( request.getRequestID() == InvitationRequest.LINK_EXISTS_REQUEST )
                                                        {
                                                                class Function implements application.net.Run
                                                                {
                                                                        @Override
                                                                        public Convertible run( Convertible obj )
                                                                        {
                                                                                try
                                                                                {
                                                                                        final Invitation invitation = ( Invitation ) obj; 
                                                                                        boolean result = Database.linkExists( con, 
                                                                                                                              invitation.getUser(),
                                                                                                                              invitation.getFriend() );
                                                                                        Bool bool = new Bool();
                                                                                        bool.setBoolean( result );
                                                                                        return bool;
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }
                                                                
                                                                if ( sendObjectManager.add( p ) )
                                                                        new SendObjectThread( network, p, new Function() ).start();                                                                
                                                        }
                                                        else if ( request.getRequestID() == InvitationRequest.INVITATION_REQUEST )
                                                        {
                                                                class Function implements application.net.Run
                                                                {
                                                                        @Override
                                                                        public Convertible run( Convertible obj )
                                                                        {
                                                                                try
                                                                                {
                                                                                        final Invitation invitation = ( Invitation ) obj;                                                                                                                                                                             
                                                                                        InvitationResponse ir = Database.invite( con, invitation );  
                                                                                        InvitationResult invitationResult = new InvitationResult();
                                                                                        
                                                                                        if ( ir == InvitationResponse.LINK_NOT_EXISTS )
                                                                                        {
                                                                                                new ServerThread()
                                                                                                {
                                                                                                        @Override
                                                                                                        public void run()
                                                                                                        {
                                                                                                                 try
                                                                                                                 {
                                                                                                                        Invitation ivtt = new Invitation();
                                                                                                                        ivtt.setUser( invitation.getFriend() );
                                                                                                                        ivtt.setFriend( invitation.getUser() );
                                                                                                                        inviteIfConnected( ivtt );
                                                                                                                 }
                                                                                                                 catch ( Exception e )
                                                                                                                 {
                                                                                                                         throw new InternalError( e.toString() );
                                                                                                                 }
                                                                                                        }
                                                                                                }.start();
                                                                                                
                                                                                                invitationResult.setBoolean( true );
                                                                                        }
                                                                                        else if ( ir == InvitationResponse.LINK_EXISTS )
                                                                                                invitationResult.setBoolean( true );                                                                                             
                                                                                        else if ( ir == InvitationResponse.INVITATION_EXISTS )
                                                                                        {
                                                                                                invitationResult.setBoolean( false );
                                                                                                invitationResult.setMessage( "Você já fez um convite para o/a " + invitation.getFriend() + " !\n"
                                                                                                                           + " Aguarde a resposta!" );
                                                                                        }
                                                                                        else if ( ir == InvitationResponse.IS_FRIEND )
                                                                                        {
                                                                                                invitationResult.setBoolean( false );
                                                                                                invitationResult.setMessage( invitation.getFriend() 
                                                                                                                             + " já está cadastrado como seu/sua amigo(a)!" );                                                                                                
                                                                                        }
                                                                                        else if ( ir == InvitationResponse.NICKNAME_NOT_EXISTS )
                                                                                        {
                                                                                                invitationResult.setBoolean( false );
                                                                                                invitationResult.setMessage( invitation.getFriend() 
                                                                                                                             + " não está cadastrado(a) no servidor!" );                                                                                         
                                                                                        }
                                                                                        
                                                                                        return invitationResult;
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }
                                                                
                                                                if ( sendObjectManager.add( p ) )
                                                                        new SendObjectThread( network, p, new Function() ).start();
                                                        }
                                                        else if ( request.getRequestID() == InvitationRequest.INVITATION_ANSWER_REQUEST )
                                                        {
                                                                class Function implements DownloadFunction
                                                                {
                                                                        @Override
                                                                        public void run( byte[] information )
                                                                        {
                                                                                try
                                                                                {
                                                                                        Invitation invitation = ( Invitation ) Convertible.bytesToObject( information );                                                                                       
                                                                                        final String user = invitation.getUser();
                                                                                        final String friend = invitation.getFriend();       

                                                                                        if ( invitation.getAnswer() == Invitation.ACCEPTED )
                                                                                                Database.accept( con, user, friend );
                                                                                        else if ( invitation.getAnswer() == Invitation.DECLINED )   
                                                                                        {
                                                                                                Database.decline( con, friend, user );
                                                                                                UserStatus us = users.getUserStatus( friend );
                                                                                                
                                                                                                if ( us != null )
                                                                                                {
                                                                                                        Invitation ivtt = new Invitation();
                                                                                                        ivtt.setUser( friend );
                                                                                                        ivtt.setFriend( user );
                                                                                                        ivtt.setAnswer( Invitation.DECLINED );
                                                                                                        Network net = new Network();
                                                                                                        final Object lock = users.getDefaultUsersSync();
                                                                                                        final InetAddress address;
                                                                                                        final int port;

                                                                                                        synchronized ( lock )
                                                                                                        {
                                                                                                                address = us.getAddress();
                                                                                                                port = us.getPort();
                                                                                                        }     
                                                                                                        
                                                                                                        Packet packet = Packet.forUpload( address,
                                                                                                                                          port,
                                                                                                                                          new InvitationRequest( InvitationRequest.INVITATION_ANSWER_REQUEST ) );
                                                                                                        
                                                                                                        net.getUploadManager().add( packet );
                                                                                                        boolean result = net.upload( packet, ivtt.toBytes() );

                                                                                                        if ( result == false )
                                                                                                                Database.postAsDeclined( con, user, friend );
                                                                                                }
                                                                                                else
                                                                                                        Database.postAsDeclined( con, user, friend );
                                                                                        }
                                                                                        else
                                                                                                throw new InternalError( "invitation.getAnswer() inválido!" );
                                                                                        
                                                                                        if ( invitation.getAnswer() == Invitation.ACCEPTED )
                                                                                        {
                                                                                                UserStatus first = users.getUserStatus( user );
                                                                                                UserStatus second = users.getUserStatus( friend );     
                                                                                                Network net = new Network();       
                                                                                                final boolean firstResult;
                                                                                                
                                                                                                if ( first != null )
                                                                                                {
                                                                                                        final Object lock = users.getDefaultUsersSync();
                                                                                                        final InetAddress address;
                                                                                                        final int port;

                                                                                                        synchronized ( lock )
                                                                                                        {
                                                                                                                address = first.getAddress();
                                                                                                                port = first.getPort();
                                                                                                        }                                                                                                        
                                                                                                        
                                                                                                        Packet firstPacket = Packet.forUpload( address,
                                                                                                                                               port,
                                                                                                                                               new InvitationRequest( InvitationRequest.INVITATION_ANSWER_REQUEST ));                                                                                                        
                                                                                                        
                                                                                                        Invitation firstInvitation = new Invitation();
                                                                                                        firstInvitation.setUser( user );
                                                                                                        firstInvitation.setFriend( friend );
                                                                                                        firstInvitation.setAnswer( Invitation.ACCEPTED );
                                                                                                        net.getUploadManager().add( firstPacket );
                                                                                                        firstResult = net.upload( firstPacket, firstInvitation.toBytes() );
                                                                                                }
                                                                                                else
                                                                                                        firstResult = true;
                                                                                                
                                                                                                final boolean secondResult;
                                                                                                
                                                                                                if ( second != null )
                                                                                                {
                                                                                                        final Object lock = users.getDefaultUsersSync();
                                                                                                        final InetAddress address;
                                                                                                        final int port;

                                                                                                        synchronized ( lock )
                                                                                                        {
                                                                                                                address = second.getAddress();
                                                                                                                port = second.getPort();
                                                                                                        }                                                                                                        
                                                                                                        
                                                                                                        Packet secondPacket = Packet.forUpload( address,
                                                                                                                                                port,
                                                                                                                                                new InvitationRequest( InvitationRequest.INVITATION_ANSWER_REQUEST ) );                                                                                                        
                                                                                                        
                                                                                                        Invitation secondInvitation = new Invitation();
                                                                                                        secondInvitation.setUser( friend );
                                                                                                        secondInvitation.setFriend( user );
                                                                                                        secondInvitation.setAnswer( Invitation.ACCEPTED );
                                                                                                        net.getUploadManager().add( secondPacket );
                                                                                                        secondResult = net.upload( secondPacket,  secondInvitation.toBytes() );
                                                                                                }
                                                                                                else
                                                                                                        secondResult = true;
                                                                                                
                                                                                                RequestSocket rs = Network.createDefaultRequestSocket();                                                                                        
                                                                                       
                                                                                                if ( firstResult && secondResult )
                                                                                                {
                                                                                                        if ( first != null )
                                                                                                        {
                                                                                                                final Object lock = users.getDefaultUsersSync();
                                                                                                                final InetAddress address;
                                                                                                                final int port;

                                                                                                                synchronized ( lock )
                                                                                                                {
                                                                                                                        address = first.getAddress();
                                                                                                                        port = first.getPort();
                                                                                                                }    
                                                                                                                
                                                                                                                Packet packet = Packet.createSmartRequest( address,
                                                                                                                                                           port,
                                                                                                                                                           new UpdateRequest( UpdateRequest.SCREEN_UPDATE_REQUEST ),
                                                                                                                                                           friend.getBytes() );
                                                                                                                rs.request( packet );
                                                                                                        }
                                                                                                        
                                                                                                        if ( second != null )
                                                                                                        {
                                                                                                                final Object lock = users.getDefaultUsersSync();
                                                                                                                final InetAddress address;
                                                                                                                final int port;

                                                                                                                synchronized ( lock )
                                                                                                                {
                                                                                                                        address = second.getAddress();
                                                                                                                        port = second.getPort();
                                                                                                                }                                                                                                                
                                                                                                                
                                                                                                                Packet packet = Packet.createSmartRequest( address,
                                                                                                                                                           port,
                                                                                                                                                           new UpdateRequest( UpdateRequest.SCREEN_UPDATE_REQUEST ),
                                                                                                                                                           user.getBytes() );
                                                                                                                rs.request( packet );      
                                                                                                        }
                                                                                                }
                                                                                        }
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }
                                                                
                                                                if ( downloadManager.add( p ) )
                                                                        new DownloadThread( network, p, new Function() ).start();
                                                        }                                                               
                                                }
                                                else if ( request instanceof UpdateRequest )
                                                {                        
                                                        if ( request.getRequestID() == UpdateRequest.CONNECTED_USER_REQUEST )
                                                        {
                                                                class Function implements DownloadFunction
                                                                {
                                                                        @Override
                                                                        public void run( byte[] information )
                                                                        {
                                                                                try
                                                                                {
                                                                                        XStatus xst = ( XStatus ) Convertible.bytesToObject( information );
                                                                                        UserStatus us = users.getUserStatus( xst.getNickName() );    
                                                                                        final St st = new St();
                                                                                        st.setNickName( xst.getNickName() );
                                                                                        st.setStatus( UserStatus.OFF_LINE ); 
                                                                                        
                                                                                        if ( us == null )                                                                                                 
                                                                                                notifyAllFriends( st );
                                                                                        else
                                                                                        {
                                                                                                final boolean connected = isConnected( us );
                                                                                                
                                                                                                if ( connected )
                                                                                                {          
                                                                                                        final Object lock = users.getDefaultUsersSync();
                                                                                                        final InetAddress usAddress;
                                                                                                        final int usPort;
                                                                                                        final int usStatus;

                                                                                                        synchronized ( lock )
                                                                                                        {
                                                                                                                usAddress = us.getAddress();
                                                                                                                usPort = us.getPort();
                                                                                                                usStatus = us.getStatus();
                                                                                                        }                                                                                                        
                                                                                                        
                                                                                                        if ( ! Network.isEquals( usAddress, InetAddress.getByAddress( xst.getAddress() ) ) ||
                                                                                                             usPort != xst.getMainPort()                                                   ||
                                                                                                             usStatus != xst.getStatus() )
                                                                                                        {
                                                                                                                UserStatus ustt = users.getUserStatus( xst.getRecipient() );
                                                                                                                
                                                                                                                if ( ustt != null )
                                                                                                                {
                                                                                                                        XStatus xstt = XStatus.UserStatusToXStatus( us );
                                                                                                                        xstt.setRecipient( xst.getRecipient() );                                                                                                                        
                                                                                                                        final InetAddress usttAddress;
                                                                                                                        final int usttPort;

                                                                                                                        synchronized ( lock )
                                                                                                                        {
                                                                                                                                usttAddress = ustt.getAddress();
                                                                                                                                usttPort = ustt.getPort();
                                                                                                                        }                                                                                                                         
                                                                                                                        
                                                                                                                        Server.updateProps( xstt, 
                                                                                                                                            usttAddress, 
                                                                                                                                            usttPort );
                                                                                                                }
                                                                                                        }                     
                                                                                                }
                                                                                                else 
                                                                                                {
                                                                                                        users.removeUserStatus( xst.getNickName() );
                                                                                                        notifyAllFriends( st );
                                                                                                        updateUsersGUI();
                                                                                                }
                                                                                        }
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }
                                                                
                                                                if ( downloadManager.add( p ) )
                                                                        new DownloadThread( network, p,  new Function() ).start();
                                                        }                                                                
                                                        else if ( request.getRequestID() == UpdateRequest.LOGON_REQUEST )
                                                        {                                                                   
                                                                class Function implements application.net.Run
                                                                {                                                                                       
                                                                        @Override
                                                                        public Convertible run( Convertible obj )
                                                                        {                                                                                       
                                                                                try
                                                                                {
                                                                                         final Logon logon = ( Logon ) obj;
                                                                                         
                                                                                         {
                                                                                                 LogonResult logonResult = Database.checkNickNameAndPassword( con, 
                                                                                                                                                              logon.getNickName(), 
                                                                                                                                                              logon.getPassword() ); 
                                                                                                 
                                                                                                 if ( ! logonResult.isConnected() )
                                                                                                         return logonResult;
                                                                                         }
  
                                                                                        
                                                                                         {
                                                                                                 UserStatus us = Useful.createDefaultUserStatus( logon.getNickName(),
                                                                                                                                                 p.getAddress(),
                                                                                                                                                 logon.getMainPort(),
                                                                                                                                                 logon.getStatus()
                                                                                                                                              );                                                                                                   
                                                                                                 
                                                                                                 UserStatus userStatus = users.getUserStatus( logon.getNickName() );    
                                                                                                 LogonResult logonResult = new LogonResult();
                                                                                                 XStatus xst = XStatus.UserStatusToXStatus( us );                                                                                          

                                                                                                 if ( userStatus == null )          
                                                                                                 {
                                                                                                        addUS( us );
                                                                                                        notifyAllFriends( xst ); 
                                                                                                        updateUsersGUI(); 
                                                                                                        logonResult.setConnected( true );
                                                                                                        checkInvitations( us.getNickName() );
                                                                                                        checkDeclined( us.getNickName() );
                                                                                                 }
                                                                                                 else
                                                                                                 {
                                                                                                         final boolean connected = isConnected( userStatus );

                                                                                                         if ( connected )
                                                                                                         {
                                                                                                                 logonResult.setConnected( false );
                                                                                                                 logonResult.setMessage( us.getNickName() + " já está conectado(a) no servidor" );
                                                                                                         }
                                                                                                         else
                                                                                                         {
                                                                                                         
                                                                                                                 synchronized ( users.getDefaultUsersSync() )
                                                                                                                 {
                                                                                                                        users.removeUserStatus( logon.getNickName() );
                                                                                                                        addUS( us );                                                                                                                
                                                                                                                 }
                                                                                                         
                                                                                                                 notifyAllFriends( xst ); 
                                                                                                                 updateUsersGUI(); 
                                                                                                                 logonResult.setConnected( true );
                                                                                                                 checkInvitations( us.getNickName() );
                                                                                                                 checkDeclined( us.getNickName() );
                                                                                                         }
                                                                                                 }

                                                                                                 return logonResult;
                                                                                         }
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }

                                                                if ( sendObjectManager.add( p ) )
                                                                {
                                                                        Function function = new Function();
                                                                        ServerThread thread = new SendObjectThread( network, p, function );
                                                                        thread.start(); 
                                                                }
                                                                        
                                                        }
                                                        else if ( request.getRequestID() == UpdateRequest.LOGOFF_REQUEST )
                                                        {
                                                                class Function implements DownloadFunction
                                                                {
                                                                        @Override
                                                                        public void run( byte[] information )
                                                                        {
                                                                                try
                                                                                {
                                                                                        Status stt = ( Status ) Convertible.bytesToObject( information );
                                                                                        UserStatus us = users.getUserStatus( stt.getNickName() );
                                                                                        
                                                                                        if ( us != null )
                                                                                        {             
                                                                                                final Object lock = users.getDefaultUsersSync();
                                                                                                final InetAddress address;
                                                                                                final int port;
                                                                                                
                                                                                                synchronized ( lock )
                                                                                                {
                                                                                                        address = us.getAddress();
                                                                                                        port = us.getPort();
                                                                                                }
                                                                                                
                                                                                                if ( Network.isEquals( address, p.getAddress() ) &&
                                                                                                     port == stt.getMainPort()   )
                                                                                                {
                                                                                                        if ( us.getStatus() == UserStatus.ON_LINE  )
                                                                                                        {
                                                                                                                St status  = new St();
                                                                                                                status.setNickName( stt.getNickName() );
                                                                                                                status.setStatus( UserStatus.OFF_LINE );
                                                                                                                notifyAllFriends( status );
                                                                                                        }

                                                                                                        users.removeUserStatus( stt.getNickName() );
                                                                                                        updateUsersGUI();       
                                                                                                }
                                                                                        }
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }
                                                                
                                                                if ( downloadManager.add( p ) )
                                                                        new DownloadThread( network, p, new Function() ).start();
                                                        }
                                                        else if ( request.getRequestID() == UpdateRequest.USER_STATUS_REQUEST )
                                                        {                                                         
                                                                class Function implements application.net.Run
                                                                {
                                                                        @Override
                                                                        public Convertible run( Convertible obj )
                                                                        {
                                                                                try
                                                                                {
                                                                                        NickName nick = ( NickName ) obj;
                                                                                        UserStatus us = users.getUserStatus( nick.getNickName() );                                                                            
                                                                                        final St inf;
                                                                                        
                                                                                        if ( us != null ) 
                                                                                                inf = XStatus.UserStatusToXStatus( us );                                                                                       
                                                                                        else
                                                                                        {
                                                                                                inf = new St();
                                                                                                inf.setNickName( nick.getNickName() );
                                                                                                inf.setStatus( UserStatus.OFF_LINE );
                                                                                        }
                                                                                        
                                                                                        return inf;
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError();
                                                                                }
                                                                        }
                                                                }
   
                                                                if ( sendObjectManager.add( p ) )
                                                                        new SendObjectThread( network, p, new Function() ).start();
                                                        }
                                                        else if ( request.getRequestID() == UpdateRequest.STATUS_UPDATE_REQUEST )
                                                        {
                                                                class Function implements DownloadFunction
                                                                {
                                                                        @Override
                                                                        public void run( byte[] information )
                                                                        {
                                                                                try
                                                                                {                                                                                      
                                                                                        Status status = ( Status ) Convertible.bytesToObject( information );
                                                                                        UserStatus us = users.getUserStatus( status.getNickName() );

                                                                                        if ( us != null )
                                                                                        {
                                                                                                final boolean connected = isConnected( us );
                                                                                                final Object lock = users.getDefaultUsersSync();
                                                                                                final InetAddress usAddress;
                                                                                                final int usPort;

                                                                                                synchronized ( lock )
                                                                                                {
                                                                                                        usAddress = us.getAddress();
                                                                                                        usPort = us.getPort();
                                                                                                } 
                                                                                                                                                                                               
                                                                                                if ( connected &&
                                                                                                 ( ! Network.isEquals( usAddress, p.getAddress() ) ||
                                                                                                     usPort != status.getMainPort() )
                                                                                                        )
                                                                                                {
                                                                                                        finalizeClient( status.getNickName(), 
                                                                                                                        p.getAddress(), 
                                                                                                                        status.getMainPort() );
                                                                                                }
                                                                                                else
                                                                                                {
                                                                                                        synchronized ( lock )
                                                                                                        {
                                                                                                                us.setStatus( status.getStatus() );                  
                                                                                                                us.setAddress( p.getAddress() );
                                                                                                                us.setPort( status.getMainPort() );
                                                                                                        }

                                                                                                        updateUsersGUI();
                                                                                                        XStatus xst = XStatus.UserStatusToXStatus( us );
                                                                                                        notifyAllFriends( xst );                                                                                                 
                                                                                                }
                                                                                        }
                                                                                        else
                                                                                        {
                                                                                                UserStatus userStatus = Useful.createDefaultUserStatus( status.getNickName(),
                                                                                                                                                        p.getAddress(),
                                                                                                                                                        status.getMainPort(),
                                                                                                                                                        status.getStatus()
                                                                                                                                                        );   
                                                                                                XStatus xst = XStatus.UserStatusToXStatus( userStatus );
                                                                                                addUS( userStatus );
                                                                                                notifyAllFriends( xst ); 
                                                                                                updateUsersGUI(); 
                                                                                        }
                                                                                }
                                                                                catch ( Exception e )
                                                                                {
                                                                                        throw new InternalError( e.toString() );
                                                                                }
                                                                        }
                                                                }
   
                                                                if ( downloadManager.add( p ) )
                                                                {
                                                                        Thread th = new DownloadThread( network, p, new Function() );
                                                                        th.setName("UpdateRequest.STATUS_UPDATE_REQUEST");
                                                                        th.start();   
                                                                }
                                                        }
                                                }                                
                                        }
                                }
                                catch ( InvalidPacketFormatException e )
                                {}
                                catch ( Exception e )
                                {                                       
                                        if ( e instanceof NetworkException )
                                                throw new InternalError( e.toString() );                                        
                                        
                                        if ( ps.isClosed() )
                                                return;
                                }
                        }
                        
                        System.out.println("mainThread terminou!");
                }
        }
        
        private static PacketSocket getPacketSocket()
        {
                for ( int iPort = Application.BEGIN_PORT; iPort <= Application.END_PORT; iPort++ )
                {
                        try
                        {
                                return  new PacketSocket( iPort, Network.DEFAULT_MAX_LENGTH );
                        }
                        catch ( Exception e )
                        {}
                }
                        
                return null;
        }
                
        private ServerThread mainThread;        
        
        private void run() 
        {
                synchronized ( executionLock )
                {
                        if ( state == RUNNING )
                                throw new RuntimeException( "O " + Application.APPLICATION_NAME + " já está em execução." );
                        
                        try
                        {
                                usersGUI = new UsersGUI();
                                users = usersGUI.getDefaultUsers();
                                
                                if ( ps != null )
                                        ps.close();

                                ps = getPacketSocket();               

                                if ( ps == null ){
                                        showMessageDialog( null, Application.APPLICATION_NAME + " não pode ser executado, "
                                                                 + "porque o intervalo de portas " + Application.BEGIN_PORT + " a " + Application.END_PORT
                                                                 + "estão sendo usadas por outros aplicativos." );
                                        
                                        return;
                                }

                                con = Database.loadConnection( InetAddress.getLocalHost().getHostAddress(), 
                                                                                              pathDB, 
                                                                                              "SYSDBA", 
                                                                                              "masterkey" );               
                                state = RUNNING;
                                mainThread = new MainThread();
                                mainThread.setPriority( ServerThread.MAX_PRIORITY );
                                mainThread.setName( mainThread.getClass().getSimpleName() );
                                mainThread.start();
                        }
                        catch ( SQLException e )
                        {
                                showMessageDialog( null, "Verifique se o Firebird 2.1 está em execução." );
                        }
                        catch ( SocketException e )
                        {
                                showMessageDialog( null, e.getMessage() );
                        }
                        catch ( Exception e )
                        {
                                showMessageDialog( null, e.getMessage() );
                        }
                }
        }    
        
        private void finalizeMainThread() throws Exception
        {
                InetAddress psHost = InetAddress.getLocalHost();
                final int psPort   = ps.getLocalPort();
                Packet finalizePacket = Packet.createSmartRequest( psHost,
                                                                   psPort,
                                                                   new UpdateRequest( UpdateRequest.SERVER_CLOSING_REQUEST ),
                                                                   new byte[]{} );
                RequestSocket requestSocket = Network.createDefaultRequestSocket();
                requestSocket.request( finalizePacket );
        }
        
        private void close()
        {
                synchronized ( executionLock )
                {
                        if ( state == STOPPED )
                                throw new RuntimeException( "O servidor não está em execução!" );
                        
                        state = STOPPED;
                        trayIcon.displayMessage( "", 
                                                 "Espere todos os processos terminarem,\n"
                                                 + " para que depois, se necessário, \n"
                                                 + "encerrar a sessão ou desligar o computador.",
                                                 TrayIcon.MessageType.WARNING );                          

                        try
                        {                      
                                finalizeMainThread();
                                ps.close();
                                joinAndRemoveAll();
                                con.close();                                    
                                
                                synchronized ( users.getDefaultUsersSync() )
                                {

                                        for ( int i = 0; i < users.getUserStatusCount(); i++ )
                                        {
                                                UserStatus us    = users.getUserStatus( i );
                                                RequestSocket rs = Network.createDefaultRequestSocket();
                                                final Object lock = users.getDefaultUsersSync();
                                                final InetAddress address;
                                                final int port;

                                                synchronized ( lock )
                                                {
                                                        address = us.getAddress();
                                                        port = us.getPort();
                                                }                                                 

                                                Packet packet    = Packet.createSmartRequest( address,
                                                                                              port,
                                                                                              new UpdateRequest( UpdateRequest.SERVER_CLOSING_REQUEST ),
                                                                                              new byte[]{} );
                                                rs.request( packet );
                                        }
                                }
                        }
                        catch ( Exception e )
                        {
                                throw new InternalError( e.toString() );
                        }                                                  
                        
                        ps = null;
                        con = null;
                        mainThread = null;
                        usersGUI.setVisible( false );
                        usersGUI = null;
                        users = null;
                        
                        trayIcon.displayMessage( "", 
                                                 "Todos os processos foram concluídos!",
                                                 TrayIcon.MessageType.INFO );     
                        
                        try
                        {
                            Thread.sleep(2000l);
                        }
                        catch ( Exception e )
                        {
                            throw new InternalError(e.toString());
                        }
                }
        }
}