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
package application.db;

import application.InvitationResponse;
import application.forms.util.AbstractDefaultUserStatus;
import application.objects.Friends;
import application.objects.Invitation;
import application.objects.LogonResult;
import application.objects.Register;
import application.objects.RegisterResult;
import application.util.Blocker;
import application.util.Executable;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Database
{        
        private static final Blocker blocker = new Blocker();
        
        static
        {
                try
                {
                        blocker.init();
                        Class.forName( "org.firebirdsql.jdbc.FBDriver" );       
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        }        
        
        public static Friends getFriends( final Connection con, 
                                          final String nickName )
        {
                if ( nickName == null )
                        throw new IllegalArgumentException( "nickName é null" );
                
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );
                
                class Exe extends Executable
                {
                        private Friends friends;
                        
                        public Friends getFriends()
                        {
                                return friends;
                        }
                        
                        @Override
                        public void run()
                        {
                                try
                                {

                                        String sql = " SELECT US_FR.NICK_NAME AS \"NICK_NAME\""
                                                   + " FROM USERS US,"
                                                   + " USERS US_FR,"
                                                   + " FRIENDS FR1,"
                                                   + " FRIENDS FR2"
                                                   + " WHERE TRIM( LOWER( US.NICK_NAME ) ) = '" + nickName.toLowerCase().trim() + "'"
                                                   + " AND US.CODE                         = FR1.USER_CODE"
                                                   + " AND US_FR.CODE                      = FR1.FRIEND_CODE"
                                                   + " AND FR1.FRIEND_CODE                 = FR2.USER_CODE"
                                                   + " AND FR2.FRIEND_CODE                 = US.CODE"
                                                   + " ORDER BY US_FR.CODE";                          


                                         Statement st = con.createStatement();
                                         ResultSet rs = st.executeQuery( sql );
                                         List<String> list = new ArrayList<String>();

                                         while ( rs.next() )
                                         {
                                                 String friend = rs.getString( "NICK_NAME" );
                                                 list.add( friend );
                                         }

                                         Friends result = new Friends();
                                         result.setFriends( list );
                                         friends = result;
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
                return exe.getFriends();                
        }
        
        public static RegisterResult register( final Connection con, final Register register )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" ); 
                
                if ( register == null )
                        throw new IllegalArgumentException( "register é null" );                   
                
                class Exe extends Executable
                {
                        private RegisterResult rr;
                        
                        public RegisterResult getRegisterResult()
                        {
                                return rr;
                        }
                        
                        @Override
                        public void run()
                        {
                                try
                                {
                                        final RegisterResult registerResult = new RegisterResult();
                                        final String nickName = AbstractDefaultUserStatus.prepareNickName( register.getNickName() );

                                        if ( nickNameExists( con, nickName ) )
                                        {
                                                registerResult.setRegistered( false );
                                                registerResult.setMessage( "O Nickname " + nickName + " está sendo usado por outra pessoa!" );
                                        }
                                        else
                                        {
                                                final int newCode = Database.genCode( con, "USERS", "CODE" );
                                                String password   = new String( register.getPassword() );
                                                Statement st      = con.createStatement();
                                                final String sql  = " INSERT INTO USERS( CODE, NICK_NAME, PWORD )"
                                                                  + " VALUES( " + newCode + ", '" + nickName + "','" + password + "' )";

                                                st.executeUpdate( sql );
                                                con.commit();
                                                registerResult.setRegistered( true );
                                        }

                                        this.rr = registerResult;
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
                RegisterResult registerResult = exe.getRegisterResult();
                
                if ( registerResult == null )
                        throw new InternalError( "registerResult == null" );
                
                return registerResult;
        } 
        
        
        private static int getUserCode( Connection con, String user )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );
                
                if ( user == null )
                        throw new IllegalArgumentException( "user é null" );

                try
                {
                        String sql = " SELECT CODE"
                                   + " FROM USERS"
                                   + " WHERE TRIM( LOWER( NICK_NAME ) ) = '" + user.trim().toLowerCase() + "'";
                        
                        Statement st = con.createStatement();
                        ResultSet rs = st.executeQuery( sql );
                        rs.next();
                        final int result = rs.getInt( "CODE" );
                        return result;
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        }        
        
        private static void link( Connection con, 
                                  String user, 
                                  String friend )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" ); 

                if ( user == null )
                        throw new IllegalArgumentException( "user é null" ); 
                
                if ( friend == null )
                        throw new IllegalArgumentException( "friend é null" ); 
                
                try
                {
                        final int userCode   = getUserCode( con, user );                                                                                              
                        final int friendCode = getUserCode( con, friend );                                            
                        String update = " INSERT INTO FRIENDS( USER_CODE, FRIEND_CODE )"
                                      + " VALUES( " + userCode + ", " + friendCode + " )";

                        Statement st = con.createStatement();
                        st.executeUpdate( update );
                        con.commit();                 
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        }
        
        private static boolean privateLinkExists( Connection con, 
                                                  String user, 
                                                  String friend )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );
                
                if ( user == null )
                        throw new IllegalArgumentException( "user é null" );

                if ( friend == null )
                        throw new IllegalArgumentException( "friend é null" );

                try
                {
                        final int userCode   = getUserCode( con, user );
                        final int friendCode = getUserCode( con, friend );
                        String sql = " SELECT COUNT( * ) "
                                   + " FROM FRIENDS"
                                   + " WHERE USER_CODE = " + userCode
                                   + "   AND FRIEND_CODE = " + friendCode;
                        
                        Statement st = con.createStatement();
                        ResultSet rs = st.executeQuery( sql );
                        rs.next();
                        final int count = rs.getInt( "COUNT" );
                        
                        if ( count == 0 )
                                return false;
                        else if ( count == 1 )
                                return true;
                        else
                                throw new InternalError( "count != 0 && count != 1" );
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        } 
        
        public static boolean isFriend( final Connection con,
                                        final String firstUser,
                                        final String secondUser )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" ); 

                if ( firstUser == null )
                        throw new IllegalArgumentException( "firstUser é null" );                

                if ( secondUser == null )
                        throw new IllegalArgumentException( "secondUser é null" );     
                
                class Exe extends Executable
                {
                        private boolean bool;
                        
                        public boolean getBoolean()
                        {
                                return bool;
                        }
                        
                        @Override
                        public void run()
                        {
                                try
                                {
                                        bool = privateIsFriend( con, 
                                                                firstUser, 
                                                                secondUser );
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }  
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
                return exe.getBoolean();                
        }
        
        public static boolean linkExists( final Connection con,
                                          final String user,
                                          final String friend )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );                              

                if ( user == null )
                        throw new IllegalArgumentException( "user é null" );                              

                if ( friend == null )
                        throw new IllegalArgumentException( "friend é null" );                              
                
                class Exe extends Executable
                {
                        private boolean bool;
                        
                        public boolean getBoolean()
                        {
                                return bool;
                        }
                        
                        @Override
                        public void run()
                        {
                                try
                                {
                                        bool = privateLinkExists( con, 
                                                                  user, 
                                                                  friend );
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }  
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
                return exe.getBoolean();        
        }
        
        private static boolean privateIsFriend( final Connection con, 
                                                String firstUser, 
                                                String secondUser )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" ); 

                if ( firstUser == null )
                        throw new IllegalArgumentException( "firstUser é null" );                

                if ( secondUser == null )
                        throw new IllegalArgumentException( "secondUser é null" );                
                
                try
                {
                        if ( privateLinkExists( con, firstUser, secondUser ) && 
                             privateLinkExists( con, secondUser, firstUser ) )
                                return true;
                        else
                                return false;
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        }
        
        public static InvitationResponse invite( final Connection con, 
                                                 final Invitation invitation )
        {  
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );                              

                if ( invitation == null )
                        throw new IllegalArgumentException( "invitation é null" );                              
                
                class Exe extends Executable
                {
                        private InvitationResponse ir;
                        
                        public InvitationResponse getInvitationResponse()
                        {
                                return ir;
                        }
                        
                        @Override
                        public void run()
                        {
                                try
                                {
                                        String user = invitation.getUser();
                                        String friend = invitation.getFriend();                                       
                                        final InvitationResponse result;

                                        if ( nickNameExists( con, friend ) )
                                        {
                                                if ( privateIsFriend( con, user, friend ) )
                                                        result = InvitationResponse.IS_FRIEND;
                                                else if ( privateLinkExists( con, user, friend ) )
                                                        result = InvitationResponse.INVITATION_EXISTS;
                                                else if ( privateLinkExists( con, friend, user ) )
                                                {
                                                        link( con, user, friend );
                                                        result = InvitationResponse.LINK_EXISTS;
                                                }
                                                else
                                                {
                                                        link( con, user, friend );
                                                        result = InvitationResponse.LINK_NOT_EXISTS; 
                                                }
                                        }
                                        else
                                                result = InvitationResponse.NICKNAME_NOT_EXISTS;
                                        
                                        ir = result;
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }  
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
                return exe.getInvitationResponse();
        } 

        public static void deleteFriend( final Connection con, 
                                         final String user,
                                         final String friend )
        {  
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );                              

                if ( user == null )
                        throw new IllegalArgumentException( "user é null" );                              

                if ( friend == null )
                        throw new IllegalArgumentException( "friend é null" );                              
                
                class Exe extends Executable
                {
                        @Override
                        public void run()
                        {                                    
                                delete( con, user, friend );
                                delete( con, friend, user ); 
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
        } 
        
        public static void accept( final Connection con, 
                                   final String firstUser, 
                                   final String secondUser )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );                              

                if ( firstUser == null )
                        throw new IllegalArgumentException( "firstUser é null" );                  

                if ( secondUser == null )
                        throw new IllegalArgumentException( "secondUser é null" );                  
                
                class Exe extends Executable
                {
                        
                        @Override
                        public void run()
                        {
                                try
                                {             
                                        if ( ! privateLinkExists( con, firstUser, secondUser ) )
                                                link( con, firstUser, secondUser );
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
        }  
        
        private static void delete( Connection con,
                                    String firstUser,
                                    String secondUser)
        {
                try
                {
                        final int firstCode = Database.getUserCode( con, firstUser );
                        final int secondCode = Database.getUserCode( con, secondUser );
                        String update = " DELETE "
                                      + " FROM FRIENDS"
                                      + " WHERE USER_CODE = " + firstCode
                                      + "   AND FRIEND_CODE = " + secondCode;

                        Statement st = con.createStatement();
                        st.executeUpdate( update );
                        con.commit();                 
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        }

        public static void decline( final Connection con, 
                                    final String firstUser, 
                                    final String secondUser )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );                              

                if ( firstUser == null )
                        throw new IllegalArgumentException( "firstUser é null" );                  

                if ( secondUser == null )
                        throw new IllegalArgumentException( "secondUser é null" );                  
                
                class Exe extends Executable
                {
                        
                        @Override
                        public void run()
                        {
                                delete( con, firstUser, secondUser );
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
        }     
        
        private static boolean exists( Connection con,
                                       String table,
                                       String firstField,
                                       String secondField,
                                       int firstCode,
                                       int secondCode )
        {
                try
                {
                        Statement st = con.createStatement();
                        String sql = " SELECT COUNT( * ) "
                                   + " FROM " + table
                                   + " WHERE " + firstField + " = " + firstCode
                                   + "   AND " + secondField + " = " + secondCode;
                        
                        ResultSet rs = st.executeQuery( sql );
                        rs.next();
                        final int count = rs.getInt( "COUNT" );
                        
                        if ( count == 1 )
                                return true;
                        else if ( count == 0 )  
                                return false; 
                        else
                                throw new InternalError( "count != 1 && count != 0" );
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        }
     
        public static void postAsDeclined( final Connection con, 
                                           final String firstUser, 
                                           final String secondUser )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );                              

                if ( firstUser == null )
                        throw new IllegalArgumentException( "firstUser é null" );                  

                if ( secondUser == null )
                        throw new IllegalArgumentException( "secondUser é null" );                  
                
                class Exe extends Executable
                {
                        
                        @Override
                        public void run()
                        {
                                try
                                {             
                                        final int firstCode = Database.getUserCode( con, firstUser );
                                        final int secondCode = Database.getUserCode( con, secondUser );
                                        
                                        if ( ! exists( con, 
                                                       "DECLINED_INVITATIONS", 
                                                       "FIRST_CODE", 
                                                       "SECOND_CODE", 
                                                       firstCode, 
                                                       secondCode ) )
                                        {
                                                String update = " INSERT "
                                                              + " INTO DECLINED_INVITATIONS( FIRST_CODE, SECOND_CODE )"
                                                              + " VALUES( " + firstCode + ", " + secondCode + " )";

                                                Statement st = con.createStatement();
                                                st.executeUpdate( update );
                                                con.commit(); 
                                        }
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
        }   
        
        public static List<String> getDeclineds( final Connection con, 
                                                 final String nickName )
        {
                if ( nickName == null )
                        throw new IllegalArgumentException( "nickName é null" );
                
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );
                
                class Exe extends Executable
                {
                        private List<String> declineds;
                        
                        public List<String> getDeclineds()
                        {
                                return declineds;
                        }
                        
                        @Override
                        public void run()
                        {
                                try
                                {

                                        String sql = " SELECT F.NICK_NAME AS \"FRIEND\""
                                                   + " FROM USERS U, USERS F, DECLINED_INVITATIONS D_I"
                                                   + " WHERE TRIM( LOWER( U.NICK_NAME ) ) = '" + nickName.toLowerCase().trim() + "'"
                                                   + "   AND U.CODE = D_I.SECOND_CODE"
                                                   + "   AND F.CODE = D_I.FIRST_CODE";


                                         Statement st = con.createStatement();
                                         ResultSet rs = st.executeQuery( sql );
                                         List<String> list = new ArrayList<String>();

                                         while ( rs.next() )
                                         {
                                                 String friend = rs.getString( "FRIEND" );
                                                 list.add( friend );
                                         }

                                         declineds = list;
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
                return exe.getDeclineds();                
        }        
        
        public static void deleteDeclined( final Connection con, 
                                           final String firstUser, 
                                           final String secondUser )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );                              

                if ( firstUser == null )
                        throw new IllegalArgumentException( "firstUser é null" );                  

                if ( secondUser == null )
                        throw new IllegalArgumentException( "secondUser é null" );                  
                
                class Exe extends Executable
                {
                        
                        @Override
                        public void run()
                        {
                                try
                                {             
                                        final int firstCode = Database.getUserCode( con, firstUser );
                                        final int secondCode = Database.getUserCode( con, secondUser );
                                        String update = " DELETE "
                                                      + " FROM DECLINED_INVITATIONS"
                                                      + " WHERE FIRST_CODE = " + firstCode
                                                      + "   AND SECOND_CODE = " + secondCode;

                                        Statement st = con.createStatement();
                                        st.executeUpdate( update );
                                        con.commit(); 
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
        }        
        
        private static int genCode( final Connection con, 
                                    final String table, 
                                    final String field )   
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );

                if ( table == null )
                        throw new IllegalArgumentException( "table é null" );
                
                if ( field == null )
                        throw new IllegalArgumentException( "field é null" );
                
                try
                {
                        Statement st = con.createStatement();
                        ResultSet rs = st.executeQuery( "SELECT MAX(" + field + ") AS \"NEW_CODE\"  FROM " + table );                           
                        rs.next();
                        return rs.getInt( "NEW_CODE" ) + 1;
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        }
        
        private static boolean nickNameExists( final Connection con, 
                                               final String nickName )     
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );

                if ( nickName== null )
                        throw new IllegalArgumentException( "nickName é null" );
                
                try
                {
                        Statement st = con.createStatement();
                        ResultSet rs = st.executeQuery( " SELECT COUNT( * ) AS \"NICK_NAME_COUNT\""
                                                      + " FROM USERS"
                                                      + " WHERE TRIM( LOWER( NICK_NAME ) ) = '" + nickName.toLowerCase().trim() + "'" );

                        if ( ! rs.next() )
                                throw new InternalError( "! rs.next()" );

                        final int count = rs.getInt( "NICK_NAME_COUNT" );

                        if ( count == 1 )
                                return true;
                        else if ( count == 0 )
                                return false;
                        else
                                throw new InternalError( "count != 1 && count != 0" );
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }
        }

        private static String getPassword( Connection con, 
                                           String nickName )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );

                if ( nickName== null )
                        throw new IllegalArgumentException( "nickName é null" );

                try
                {                     
                        Statement st = con.createStatement();
                        String sql = " SELECT PWORD "
                                   + " FROM USERS "
                                   + " WHERE TRIM( LOWER( NICK_NAME ) ) = '" + nickName.toLowerCase().trim() + "'";
                        
                        ResultSet rs = st.executeQuery( sql );
                        
                        if ( ! rs.next() )
                                throw new InternalError( "! rs.next()" );

                        return rs.getString( "PWORD" );
                }
                catch ( Exception e )
                {
                        throw new InternalError( e.toString() );
                }                
                
        }
        
        public static List<String> getInvitations( final Connection con, 
                                                   final String nickName )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );                

                if ( nickName == null )
                        throw new IllegalArgumentException( "nickName é null" );                               
                
                class Exe extends Executable
                {
                        private List<String> invitations;
                        
                        public List<String> getInvitations()
                        {
                                return invitations;
                        }
                        
                        @Override
                        public void run()
                        {
                                try
                                {             
                                        final List<String> result = new ArrayList<String>();
                                        final int userCode = Database.getUserCode( con, nickName );
                                        Statement st = con.createStatement();
                                        String sql = " SELECT U.NICK_NAME AS \"FRIEND\""
                                                   + " FROM FRIENDS F, USERS U"
                                                   + " WHERE F.FRIEND_CODE = " + userCode
                                                   + "   AND U.CODE = F.USER_CODE"
                                                   + "   AND 0 = ( SELECT COUNT( * ) "
                                                   + "             FROM FRIENDS "
                                                   + "             WHERE FRIENDS.USER_CODE = " + userCode
                                                   + "               AND FRIENDS.FRIEND_CODE = F.USER_CODE )";

                                        ResultSet rs = st.executeQuery( sql );

                                        while ( rs.next() )
                                        {
                                                final String friend = rs.getString( "FRIEND" );
                                                result.add( friend );
                                        }
                                        
                                        invitations = result;
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
                return exe.getInvitations();
        }
        
        public static LogonResult checkNickNameAndPassword( final Connection con, 
                                                            final String nickName, 
                                                            final char[] password )
        {
                if ( con == null )
                        throw new IllegalArgumentException( "con é null" );                

                if ( nickName == null )
                        throw new IllegalArgumentException( "nickName é null" );                
                
                if ( password == null )
                        throw new IllegalArgumentException( "password é null" );                 
                
                class Exe extends Executable
                {
                        private LogonResult lr;
                        
                        public LogonResult getLogonResult()
                        {
                                return lr;
                        }
                        
                        @Override
                        public void run()
                        {
                                try
                                {             
                                        final LogonResult result;
                                        
                                        if ( ! nickNameExists( con, nickName ) )
                                        {
                                                LogonResult logonResult = new LogonResult(); 
                                                logonResult.setConnected( false );
                                                logonResult.setMessage( "Nickname " + nickName + " não "
                                                                       + "está cadastrado no servidor!" );
                                                result = logonResult;
                                        }
                                        else
                                        {
                                                String first = new String( password );
                                                String second = getPassword( con, nickName );
                                                
                                                if ( first.trim().equals( second.trim() ) )
                                                {
                                                        LogonResult logonResult = new LogonResult();
                                                        logonResult.setConnected( true );      
                                                        result = logonResult;
                                                }
                                                else
                                                {
                                                        LogonResult logonResult = new LogonResult();
                                                        logonResult.setConnected( false );
                                                        logonResult.setMessage( "A senha está incorreta!" );
                                                        result = logonResult;
                                                }
                                        }
                                        
                                        lr = result;
                                }
                                catch ( Exception e )
                                {
                                        throw new InternalError( e.toString() );
                                }
                        }
                }
                
                Exe exe = new Exe();
                blocker.execute( exe );
                exe.expect();
                return exe.getLogonResult();
        }
        
        public static Connection loadConnection( final String ip, 
                                                 final String path, 
                                                 final String user,
                                                 final String password ) throws Exception
        {
                if ( ip == null )
                        throw new IllegalArgumentException( "ip é null" );                

                if ( path == null )
                        throw new IllegalArgumentException( "path é null" );                
                
                if ( user == null )
                        throw new IllegalArgumentException( "user é null" );                
                
                if ( password == null )
                        throw new IllegalArgumentException( "password é null" );                
                
                final String url = "jdbc:firebirdsql:" + ip + "/3050:" + path;
                Connection result = DriverManager.getConnection( url, user, password );
                result.setAutoCommit( false );
                return result;
        }        
}