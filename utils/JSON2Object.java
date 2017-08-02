package utils;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class Staff {

	public String name;
	public int age;
	public String position;
	public BigDecimal salary;
	public List<String> skills;
}

public class JSON2Object {

    
    
    public static ArrayList<String>  readDevicesFile(String fileName) {

        BufferedReader inputStream;
        ArrayList<String> jsonStrings = new ArrayList<>();

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
//                    json = json + c;
                    if ((c == '\n')||(c=='\r')) {
                        json = json + ' ';
                    } else {
                        json = json + c;
                    }
                }
                if (nbrackets == 0) {
                    if (c == '}') {
                        json = json + c;
                        System.out.println("JSON=" + json);
                        jsonStrings.add(json);
                        json = "";
                    }
                }
            }

            System.out.println("read from " + fileName + " : " + charCount + " chars");
            if (!json.equals("")){
                            System.out.println("reached end of file while parsing object : "+json);
            }
            inputStream.close();
        } catch (FileNotFoundException fnf) {
            System.out.println("file not found");
        } catch (IOException io) {
            System.out.println("io exception");
        }
        return jsonStrings;
    }
    



    public static void getObjects() {
        readDevicesFile(System.getProperty("user.home") + "\\Documents\\RGPIO\\json.txt");
        
        		ObjectMapper mapper = new ObjectMapper();

		try {

			// Convert JSON string to Object
			String jsonInString = "{\"name\":\"mkyong\",\"salary\":7500,\"skills\":[\"java\",\"python\"]}";
			Staff staff1 = mapper.readValue(jsonInString, Staff.class);
			System.out.println(staff1.name);
			System.out.println(staff1.salary);
                        			System.out.println(staff1.skills);

		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

}

