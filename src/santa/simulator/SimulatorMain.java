package santa.simulator;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

// for validation of config file against XSD.
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import java.io.InputStream;
import org.xml.sax.SAXException;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: SimulatorMain.java,v 1.2 2006/02/17 12:06:55 rambaut Exp $
 */
public class SimulatorMain {

    public static void main(String[] args) {
		Simulator simulator;

        if (args.length > 0) {
			simulator = simulatorFactory(args);

	        Logger.getLogger("santa.simulator").addHandler(new ConsoleHandler());

	        simulator.run();
        } else {
	        System.out.println("Usage: santa [-arg=value] <input_file>");
	        System.exit(0);
        }
    }

	public static Simulator simulatorFactory(String[] args) {
		Simulator simulator = null;
			
		Map<String, String> parameterValueMap = parseParameters(args);
		
		// A special parameter 'seed' is used to set the RNG seed.
		long seed = System.currentTimeMillis(); 
		if (parameterValueMap.containsKey("seed"))
			seed = Long.parseLong(parameterValueMap.get("seed"), 10);
		Random.setSeed(seed);
		System.out.println("Seed: " + seed);
			
		String systemID = "santa.xsd";
		Class currentClass = new Object() { }.getClass().getEnclosingClass();
		ClassLoader cl = currentClass.getClassLoader();
		InputStream in = cl.getResourceAsStream(systemID);
		Source schemaFile = new StreamSource(in);


		// URL schemaFile = new URL("http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd");
		Source xmlFile = new StreamSource(new File(args[args.length - 1]));
		try {
			SchemaFactory schemaFactory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(schemaFile);
			Validator validator = schema.newValidator();
			validator.validate(xmlFile);
			System.out.println(xmlFile.getSystemId() + " is valid");
		} catch (SAXException e) {
			System.out.println(xmlFile.getSystemId() + " is NOT valid");
			System.out.println("Reason: " + e.getLocalizedMessage());
		} catch (IOException ioe) {
			System.err.println("Error reading XML input file: " + ioe.getMessage());
			System.exit(1);
		}

		File file = new File(args[args.length - 1]);
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(file);

			SimulatorParser parser = new SimulatorParser();
			parser.setParameters(parameterValueMap );

			simulator = parser.parse(doc.getRootElement());

		} catch (SimulatorParser.ParseException pe) {
			pe.printStackTrace();
			System.err.println(pe.getMessage());
			System.exit(1);
		} catch (JDOMException jde) {
			System.err.println("Error parsing XML input file: " + jde.getMessage());
			System.exit(1);
		} catch (IOException ioe) {
			System.err.println("Error reading XML input file: " + ioe.getMessage());
			System.exit(1);
		}

		return simulator;
	}


    private static Map<String, String> parseParameters(String[] args) {
        Map<String, String> parameterValueMap = new HashMap<String, String>();

        for (int i = 0; i < args.length - 1; ++i) {
            String arg = args[i];

            if (arg.charAt(0) != '-') {
                System.err.println("Do not understand '" + arg + "': should be -arg=value");
                System.exit(0);
            }

            String[] argvalue = arg.substring(1).split("=");

            if (argvalue.length != 2) {
                System.err.println("Do not understand '" + arg + "': should be -arg=value");
                System.exit(0);
            }

            parameterValueMap.put(argvalue[0], argvalue[1]);
        }

        return parameterValueMap;
    }
}
