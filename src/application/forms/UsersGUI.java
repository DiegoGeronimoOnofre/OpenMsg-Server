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

import application.forms.util.Useful;
import application.forms.util.Users;

import java.awt.BorderLayout;
import java.awt.Point;
import javax.swing.JFrame;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class UsersGUI extends JFrame
{
        private final Users users = Useful.createDefaultUsers();       
        
        final JPanel pnlInfo = new JPanel()
        {
            private final JLabel lblInfo = new JLabel();
            
            {
                setPreferredSize(new Dimension(0, 15));
                setLayout(null);
                lblInfo.setLocation(0, 0);
                lblInfo.setSize(300, 15);
                this.add(lblInfo);
            }
            
            @Override
            public void paint(java.awt.Graphics g)
            {           
                int friendCount = users.getUserStatusCount();
                
                if ( friendCount == 0 )
                    lblInfo.setText("Nenhuma pessoa conectada!");
                else
                    lblInfo.setText( String.valueOf(friendCount) + " Pessoa(s) conectada(s)" );
                
                super.paint(g);    
            }
        };        
        
        public UsersGUI()
        {              
                final int w = 180;
                final int h = 350;
                Point p = new Point( 0, 0 );
                setSize( w, h );
                setLocation( p );
                setTitle( "Usuários Conectados" );   
                application.forms.util.Useful.setDefaultImageIcon( this );
                Container contentPane = getContentPane();
                contentPane.setLayout( new BorderLayout() );
                JScrollPane spn = new JScrollPane( ( ( Component ) users ), 
                                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );  
                
                contentPane.add( spn, BorderLayout.CENTER );        
                contentPane.add(pnlInfo, BorderLayout.SOUTH);
        }      
        
        public Users getDefaultUsers()
        {
                return users;
        }
}