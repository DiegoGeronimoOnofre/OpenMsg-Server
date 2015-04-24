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
package application;

public class Security 
{ 
    private static final int byteNumbers = 50;
    
    public static byte[] encrypt(byte[] value)
    {
        if ( value == null )
            throw new RuntimeException("value is null");
        
        final int resultSize = value.length * byteNumbers;
        byte[] result = new byte[resultSize];
        
        for ( int i = 0, j = 0; i < resultSize; i++ ){
            boolean isDiv = i % byteNumbers == 0;
            
            if ( isDiv ){
                result[i] = value[j];
                j++;
            }
            else{
                final byte randomByte = ((byte) (Math.random() * 127));
                result[i] = randomByte;
            }
        }
        
        return result;
    }
    
    public static byte[] decrypt(byte[] value)
    {
        if ( value == null )
            throw new RuntimeException("value is null");     
        
        final int resultSize = value.length / byteNumbers;
        byte[] result = new byte[resultSize];
        
        for ( int i = 0, j = 0; i < value.length; i++ ){
            boolean isDiv = i % byteNumbers == 0;
            
            if ( isDiv ){
                result[j] = value[i];
                j++;
            }
        }   
        
        return result;
    }
}
