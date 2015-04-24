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
package application.xml;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class ApplicationXml 
{
    private static final String path = application.Application.getMainPath() + File.separatorChar + "Application.xml";
    
    private ApplicationXml()
    {
    }
    
    public static String getApplicationVersion()
    {
        try
        {
            File xmlFile = new File(path);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            Document document = documentBuilder.parse(xmlFile);
            Element rootElement = document.getDocumentElement();
            Element versionElement = (Element) rootElement.getElementsByTagName("Version").item(0);
            return versionElement.getAttribute("value");
        }
        catch ( Exception e )
        {
            throw new InternalError(e.toString());
        }
    }
}
