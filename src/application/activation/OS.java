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

import application.Application;
import application.Security;

import java.io.File;
import java.io.RandomAccessFile;

public class OS
{
    private static final String actDir;
    
    static{
        String os = System.getProperty("os.name");
        
        if ( os.toLowerCase().startsWith("windows") )
            actDir = "C:\\windows";
        else
            actDir = Application.getMainPath();
    }
    
     /** hiddenFile oculta o arquivo
     * passado como parâmetro
     */

   public static void hiddenFile(String filePath)
   {
        try{
            String os = System.getProperty("os.name");
            
            if ( os.toLowerCase().startsWith("windows"))
                Runtime.getRuntime().exec("attrib +H \"" + filePath + "\"" ); 
        }
        catch ( Throwable t ){
            System.out.println(t.toString());
        }
   }
   
   private static String genActFileName()
   {
       int i = 0;
       
       while ( true ){
           String fileName = "dat" + String.valueOf(i) + ".dat";
           String path = actDir + File.separatorChar + fileName;
           File f = new File(path);
           
           if (! f.exists() )
               return fileName;
           
           i++;
       }
   }
   
   /*fileName é o nome do arquivo que possui
    o caminho do arquivo de ativação*/
   
   private static final String fileName = "dx.dtx";
   
   private static final Object fileLock = new Object();
   
   private static String readFilePath()
   {
       synchronized ( fileLock ){
            try{
                 RandomAccessFile randomFile = new RandomAccessFile(Application.getMainPath() + File.separatorChar + fileName, "rw");
                 //A linha de código abaixo está pulando o valor boolean
                 //que determina se o arquivo está com o caminho.
                 randomFile.seek(1);
                 final long fileLength = randomFile.length();
                 byte[] bytesValue = new byte[(int)fileLength-1]; 
                 randomFile.read(bytesValue);
                 byte[] result = Security.decrypt(bytesValue);
                 randomFile.close();
                 String path = new String(result);
                 File f = new File(path);
                 
                 if ( f.exists() )
                    return path;
                 else{
                     System.out.println(path + ":" + "Não existe!");
                     return null;
                 }
            }
            catch ( Exception e ){
                throw new RuntimeException(e.toString());
            }       
       }
   }
   
   public static void createFile()
   {
       synchronized (fileLock){
           try{
                File f = new File(Application.getMainPath() + File.separatorChar + fileName);
                
                if ( f.exists() )
                    f.delete();
                
                f.createNewFile();
                RandomAccessFile randomFile = new RandomAccessFile(f, "rw");
                randomFile.seek(0);
                randomFile.writeBoolean(false);
                randomFile.close();
           }
           catch ( Exception e ){
               throw new RuntimeException(e.toString());
           }
       }
   }
   
   public static String getActivationFilePath()
   {
       synchronized ( fileLock ){
            try{
                 RandomAccessFile randomFile = new RandomAccessFile( Application.getMainPath() + File.separatorChar + fileName, "rw");
                 randomFile.seek(0);
                 final long fileLength = randomFile.length();
                 boolean isDefined = randomFile.readBoolean();
                 
                 if ( ! isDefined ){
                     randomFile.setLength(1);
                     String fileName = genActFileName();
                     String fp = actDir + File.separatorChar + fileName;
                     byte[] bytesValue = fp.getBytes();
                     byte[] encrypted = Security.encrypt(bytesValue);
                     randomFile.write(encrypted);
                     File activationFile = new File(fp);
                     activationFile.createNewFile();
                     
                     if ( ActivationFile.defineActivationFile(activationFile)){
                        randomFile.seek(0);
                        randomFile.writeBoolean(true);
                     }
                 }

                 randomFile.close();
                 return readFilePath();
            }
            catch ( Exception e ){
                System.out.println(e.toString());
                return null;
            }
       }
   }
}
