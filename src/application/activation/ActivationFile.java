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
package application.activation;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.swing.JOptionPane;

import application.forms.Buy;

public class ActivationFile 
{
    public static final Object fileBlocker = new Object();
    
    private static final long absPosition = 15;
    
    private static final String filePath = OS.getActivationFilePath();
    
    private static final File datFile = filePath!=null ? new File(filePath) : null;
    
    private static final int maxExecutions = 100;    
    
    public static boolean isActivated()
    {
        synchronized ( fileBlocker ){
            try{        
                RandomAccessFile randomFile = new RandomAccessFile(datFile,"r");
                randomFile.seek(absPosition);
                final boolean isActivated = randomFile.readBoolean();
                randomFile.close();
                return isActivated;
            }
            catch (IOException e){
                JOptionPane.showMessageDialog(null, errorMessage);
                System.exit(0);
                throw new InternalError(e.toString());
            }
        }
    }

    public static void setActivated(boolean value)
    {
        synchronized ( fileBlocker ){
            try{        
                RandomAccessFile randomFile = new RandomAccessFile(datFile,"rw");
                randomFile.seek(absPosition);
                randomFile.writeBoolean(value);
                randomFile.close();
            }
            catch (IOException e){
                JOptionPane.showMessageDialog(null, errorMessage);
                throw new InternalError(e.toString());
            }
        }
    }
    
    private static int getExecutionsNumber()
    {
        synchronized ( fileBlocker ){
            try{
                RandomAccessFile randomFile = new RandomAccessFile(datFile,"r");
                randomFile.seek(absPosition + 1);
                final int executionsNumber = randomFile.readInt();
                randomFile.close();
                return executionsNumber;
            }
            catch ( IOException e ){
                JOptionPane.showMessageDialog(null, errorMessage);
                throw new InternalError(e.toString());
            }    
        }
    }

    private static void setExecutionsNumber(int value)
    {
        synchronized ( fileBlocker ){
            try{
                RandomAccessFile randomFile = new RandomAccessFile(datFile,"rw");
                randomFile.seek(absPosition + 1);
                randomFile.writeInt(value);
                randomFile.close();
            }
            catch ( IOException e ){
                JOptionPane.showMessageDialog(null, errorMessage);
                throw new InternalError(e.toString());
            }    
        }
    }
    
    private static boolean isExpired()
    {
        final int executionsNumber = getExecutionsNumber();
        return executionsNumber > maxExecutions;
    }    
    
    private static void incExecutionsNumber()
    {
        synchronized ( fileBlocker ){
            final int en = getExecutionsNumber();
            setExecutionsNumber(en + 1);
        }
    }
    
    public static boolean defineActivationFile( File f )
    {
        synchronized ( fileBlocker ){
            try{
                RandomAccessFile randomFile = new RandomAccessFile(f, "rw");
                byte[] array = new byte[]{1,8,12,15,13,78,45,98,45,10,12,13,14,15,16};
                randomFile.write(array,0,array.length);
                randomFile.writeBoolean(false);
                randomFile.writeInt(0);
                randomFile.write(array,0,array.length);

                for ( int i = 0; i < 2000; i++ ){
                    int value = ((int)(Math.random() * 200));
                    randomFile.write(value);
                }

                randomFile.close();
                return true;
            }
            catch ( Exception e ){
                return false;
            } 
        }
    }   
    
    public static final int IS_ACTIVATED = 0;
    public static final int IN_ANALYSIS  = 1;
    public static final int IS_EXPIRED   = 2;
    public static final int ERROR        = 3;
    
    private static final String errorMessage = "O aplicativo não pode continuar sua execução, porque\n"
                                             + "provavelmente não foi executado como administrador.\n"
                                             + "Tente executar o aplicativo em modo administrativo,\n"
                                             + "se o problema persistir, tente reinstalar a aplicação.";
    
    public static int check()
    {   
        synchronized ( fileBlocker ){
            if ( filePath == null ){
                JOptionPane.showMessageDialog(null, errorMessage);
                return ERROR;
            }
            
            if (! isActivated() )
            {
                incExecutionsNumber();

                if ( isExpired() )
                {
                    int response = JOptionPane.showConfirmDialog(null, 
                                                                "O número de execuções de teste do " + application.Application.APPLICATION_NAME + " se\n"
                                                              + "esgotaram. Para continuar utilizando este aplicativo\n"
                                                              + "é necessário ativá-lo. Deseja ativá-lo agora?\n",
                                                                "Selecione uma opção",
                                                                JOptionPane.OK_CANCEL_OPTION);

                    if ( JOptionPane.YES_OPTION == response ){
                        Buy buy = new Buy();
                        buy.setDefaultCloseOperation(Buy.EXIT_ON_CLOSE);
                        buy.setVisible(true);
                    }

                    return IS_EXPIRED;
                }
                else
                {
                    final int en = getExecutionsNumber();
                    
                    new Thread()
                    {
                        @Override
                        public void run()
                        {
                            JOptionPane.showMessageDialog(null, 
                                                          "Número de execuções restantes\n"
                                                          + "do aplicativo " + application.Application.APPLICATION_NAME + " :" 
                                                          + ( maxExecutions - en) 
                                                          + " de " + maxExecutions,
                                                          "Informação",
                                                          JOptionPane.INFORMATION_MESSAGE);
                        }
                    }.start();
                        
                    return IN_ANALYSIS;
                }
            }  
            else
                return IS_ACTIVATED;
        }
    }
}
