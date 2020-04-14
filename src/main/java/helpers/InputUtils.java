package helpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class InputUtils {
	
	private static String DELIMITER = "	";
	private static Integer STOPWORD_PRESENT = 1;
	
	public static String[] readLinesFromTextFile(String filename)
    {
        try {
            FileReader fileReader = new FileReader(filename);
            
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            List<String> lines = new ArrayList<String>();
            String line = null;
             
            while ((line = bufferedReader.readLine()) != null) 
            {
                lines.add(line);
            }
             
            bufferedReader.close();
             
            return lines.toArray(new String[lines.size()]);
        }
        catch(IOException e) {
            System.out.println("Unable to create " + filename + ": " + e.getMessage());              
        }
        
        return null;
    }
	
	public static Map<String, Integer> readLinesToMap(String filename) {
		try {
	        Map<String, Integer> map = new HashMap<>();
	        Stream<String> lines = Files.lines(Paths.get(filename));
	        lines.forEach(
                line -> map.putIfAbsent(
                		line.split(DELIMITER)[0],
                		STOPWORD_PRESENT
				)
            );
	        
	        lines.close();
	        
	        return map;
        }
        catch(IOException e) {
            System.out.println("Unable to create " + filename + ": " + e.getMessage());              
        }

        return null;
	}
	
	public static Map<String, Integer> readLinesToDictionary(String filename) {
		try {
	        Map<String, Integer> map = new HashMap<>();
	        Stream<String> lines = Files.lines(Paths.get(filename));
	        lines.filter(line -> line.contains(DELIMITER)).forEach(
	                line -> map.putIfAbsent(
	                		line.split(DELIMITER)[0],
	                		Integer.valueOf(line.split(DELIMITER)[1])
    				)
            );
	        
	        lines.close();
	        
	        return map;
        }
        catch(IOException e) {
            System.out.println("Unable to create " + filename + ": " + e.getMessage());              
        }

        return null;
	}
}
