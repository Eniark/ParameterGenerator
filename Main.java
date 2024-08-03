package createParameterSets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.text.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVReader;

import com.opencsv.exceptions.CsvValidationException;


public class Main {
	public static void main(String[] args) throws FileNotFoundException, IOException, CsvValidationException, ParseException { 
		
		String INPUT_FILE_NAME = null;
		String OUTPUT_FILE_NAME = null;
		char OUTPUT_FILE_DELIMITER = ',';
		
	
		
		if (args.length < 2) {
			
			System.out.println("Incorrect parameter amount");
			System.exit(0);
		}
		else {
			INPUT_FILE_NAME  = args[0];
			OUTPUT_FILE_NAME =  args[1];
			OUTPUT_FILE_DELIMITER = Configurations.OUTPUT_FILE_DELIMITER;			
		}

		if (INPUT_FILE_NAME == null) {
            throw new IllegalArgumentException("You must specify the input file names.");
        }
		
		
        CSVReader reader = Utilities.readCSV(INPUT_FILE_NAME, OUTPUT_FILE_DELIMITER);
        String[] header  = reader.readNext();
        
        if (header == null) {
            throw new IllegalArgumentException("The CSV file is empty or cannot be read.");
        }

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.setPrettyPrinting().create();
        Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();

        String[] line;
        List<List<String>> resultList = new ArrayList<>();

        while ((line = reader.readNext()) != null) {
            String functionName = line[0];
//            if (Configurations.DEBUG==1) {
//            	System.out.print("Function Name: ");
//                System.out.println(line[0]);
//            	System.out.print("Parameters: ");
//                System.out.println(line[1]);
//                System.out.println("====");
//            	
//            }

   
            List<Map<String, Object>> parameters = gson.fromJson(line[1], type);

            List<JsonObject> combinations = new ArrayList<>();

            Combinations.generateCombinations(parameters, new JsonObject(), 0, combinations);

            for (JsonObject jsonObject : combinations) {
                List<String> newList = new ArrayList<>();
                newList.add(functionName);
                newList.add(jsonObject.toString());
                resultList.add(newList);

            }

        }
        reader.close();
        
        if (Configurations.DEBUG==1) {
        	resultList.forEach(element -> {
        		String prettyPrintedElement = gson.toJson(element);
//        		System.out.println(prettyPrintedElement);
        	});
        }
        
        String filePath = Utilities.writeCSV(header, resultList, OUTPUT_FILE_NAME, OUTPUT_FILE_DELIMITER);
        

        System.out.println(filePath);
        System.exit(0);
    }
	
	
}
