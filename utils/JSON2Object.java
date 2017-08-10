package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;

// utility to parse a file that contains JSON objects.
// readJSONFile() reads the JSON objects and returns a list of java objects.
// Characters outside the { } braces of the JSON objects are ignored. This can be used to add comments.
// A class must exist which has as fields (must be public fields)  all the possible JSON object properties.
// This class must be given as parameter. 
// The returned objects can be cast to this class and the fields can then be used.
public class JSON2Object {

    public static ArrayList<Object> readJSONFile(String fileName, Class<?> valueType) {

        BufferedReader inputStream;
        ArrayList<Object> jsonObjects = new ArrayList<>();

        try {
            System.out.println("Opening " + fileName);
            File file = new File(fileName);
            InputStream is = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            inputStream = new BufferedReader(isr);

            int charCount = 0;
            int i;
            char c;
            String json = "";
            int nbrackets = 0;
            while ((i = inputStream.read()) != -1) {
                charCount++;
                c = (char) i;
                if (c == '{') {
                    nbrackets++;
                }
                if (c == '}') {
                    nbrackets--;
                }

                if (nbrackets > 0) {
                    if ((c == '\n') || (c == '\r')) {
                        json = json + ' ';
                    } else {
                        json = json + c;
                    }
                }
                if (nbrackets == 0) {
                    if (c == '}') {
                        json = json + c;
                        System.out.println("JSON=" + json);
                        jsonObjects.add(jsonStringToObject(json, valueType));
                        json = "";
                    }
                }
                if (nbrackets < 0) {
                    System.out.println("Extraneous } ignored");
                }
            }

            System.out.println("read from " + fileName + " : " + charCount + " chars");
            if (!json.equals("")) {
                System.out.println("reached end of file while parsing object : " + json);
            }
            inputStream.close();
        } catch (FileNotFoundException fnf) {
            System.out.println("file not found");
        } catch (IOException io) {
            System.out.println("io exception");
        }
        return jsonObjects;
    }

    public static Object jsonStringToObject(String jsonString, Class<?> valueType) {

        Object jsonObject = null;
        ObjectMapper mapper = new ObjectMapper();

        try {
            jsonObject = mapper.readValue(jsonString, valueType);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonObject;

    }

    public static void writeJSONFile(String fileName, ArrayList<Object> objects) {

        {
            try {

                File initialFile = new File(fileName);
                OutputStream is = new FileOutputStream(initialFile);
                OutputStreamWriter isr = new OutputStreamWriter(is, "UTF-8");
                BufferedWriter outputStream = new BufferedWriter(isr);

                outputStream.write("{");
                String separator="";
                for (Object someObject : objects) {
 //                   System.out.println(":::object " + someObject.getClass().getName());
                    for (Field field : someObject.getClass().getDeclaredFields()) {
                        field.setAccessible(true); // You might want to set modifier to public first.
                        try {
//                            System.out.println(":::field " + field.getName());
                            Object value = field.get(someObject);
                            if (value != null) {
                                outputStream.write(separator+"\n\""+field.getName() + "\":\"" + value+"\"");
                          outputStream.write(" "+field.getType().getName());
                         outputStream.write(" "+field.getType().getTypeName());
                  
         
                                separator=",";
                            }
                        } catch (IllegalAccessException iae) {
                        }
                    }
                }
                outputStream.write("\n}");
                outputStream.close();

            } catch (IOException io) {
            System.out.println("io exception");
            }

        }

    }
}
