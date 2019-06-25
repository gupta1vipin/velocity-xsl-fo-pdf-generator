package com.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.DateTool;

public class PdfGeneratorUtil {

	public static void main(String[] args) {
		PdfGeneratorUtil pdfTest = new PdfGeneratorUtil();
        Map<String, Object> map = new HashMap<>();
        map.put("inputData", "Vipin");
        map.put("dateTool", new DateTool());
        map.put("testdate", new Date());
        map.put("myLocale", java.util.Locale.JAPAN);
        byte[] pdfByteArray = pdfTest.generatePdf(map, "./resources/xsl-fo-velocity-template.vm");
        
        pdfTest.writeBytesToFileNio(pdfByteArray, "./resources/result.pdf");
        
	}
	
	//Since JDK 7, NIO
    private void writeBytesToFileNio(byte[] bFile, String fileDest) {

        try {
            Path path = Paths.get(fileDest);
            Files.write(path, bFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
	
    public byte[] generatePdf(Map<String, Object> inputObjects, String templateFilePath) {
        // Source for dynamic variables
        final StreamSource source = new StreamSource(new StringReader("<root></root>"));
        // Source for file template xsl-fo
        StreamSource transformSource;
        // FOP Factory instance
        final FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());
        // FOP Agent for transformation
        final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final String body = getPdfBody(inputObjects, templateFilePath);
        if (body == null)
        {
            throw new RuntimeException("No content found for template " + templateFilePath);
        }
        transformSource = new StreamSource(new StringReader(body));
        try
        {
            final Transformer xslTransfromer = getXSLTransformerWithoutSecureFeature(transformSource);
            final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, outputStream);
            final Result res = new SAXResult((fop.getDefaultHandler()));
            xslTransfromer.transform(source, res);
        }
        catch (final Exception e)
        {
        }
        finally
        {
            try
            {
                outputStream.close();
            }
            catch (final IOException e)
            {
            }
        }
        return outputStream.toByteArray();
    }
    
	protected String getPdfBody(Map<String, Object> inputObjects, final String filePath)
    {
        StringWriter renderedBody = new StringWriter();
        Velocity.init();
        final VelocityContext velocityContext = new VelocityContext();
        inputObjects.keySet().parallelStream().forEach(key -> {
            velocityContext.put(key, inputObjects.get(key));

        });
        velocityContext.put("dateTool", new DateTool());
        Reader reader = null;
        try {
            reader = new InputStreamReader( new FileInputStream(new File(filePath)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Velocity.evaluate(velocityContext, renderedBody, "",reader);
        return renderedBody.getBuffer().toString();
    }

    private Transformer getXSLTransformerWithoutSecureFeature(final StreamSource source) throws TransformerConfigurationException
    {
        final TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        return factory.newTransformer(source);
    }

}
