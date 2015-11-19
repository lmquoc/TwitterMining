package twittermining;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TwitterMining {
	
	// For QUOC: CHANGE HERE
	private static String userDataPath = "E:/TwitterMining/data/";
	private static String userGitPath = "E:/Github/TwitterMining/";
	
	// For HUY: CHANGE HERE
//	private static String userDataPath = "";
//	private static String userGitPath = "";

	// FILE PATH 
	private static String [] folderName = {"NewYorkOneWeek", "Oscars", "ParisSearchFeb", "ParisSearchJan"};
	private static String targetFolder =  userDataPath + folderName[1]; // CHANGE idx HERE !!!!!!!
	
	private static String fileTxtOutput = userGitPath + "TwitterMining.txt";
	private static String fileEdgeCalculation = userGitPath + "EdgeCalculation.txt";

	
	// GLOBAL VARIABLES
	static List<String> arrayHashtags = new ArrayList<String>();
	static int counter = 0;	

	public static void main(String[] args) throws IOException, ParseException {		
		readFileFromFolder(targetFolder);		
		buildGraph();
	}
	
	private static void readFileFromFolder(String folderPath) throws IOException, ParseException{		
		
		
		File folder = new File(folderPath);
		File[] listOfFiles = folder.listFiles();
		
		System.out.println("Start process folder " + folder.getName());
		System.out.println("************************************");
		
		List<String> listOfFilesToProcess = new ArrayList<String>();
		
		// Run thru the folder to get the filename
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				listOfFilesToProcess.add(listOfFiles[i].getPath());
			} 
		}
		
		// Start process each file in the folder
		int flag = 0;
		while(flag < listOfFilesToProcess.size()){
			processData(listOfFilesToProcess.get(flag));
			System.out.println("Finish refining data from " + listOfFilesToProcess.get(flag));
			System.out.println(counter + " twits");
			flag++;
		}
		
		System.out.println("**********");		
		System.out.println("FINISHED " + folder.getName());
	}

	private static void processData(String jsonFilePath) throws IOException, ParseException {
		// TODO Auto-generated method stub
		FileReader myFile = new FileReader(jsonFilePath);
		BufferedReader myBuf = new BufferedReader(myFile);
		BufferedWriter myWriter = new BufferedWriter(new FileWriter(fileTxtOutput));

		JSONParser myParser = new JSONParser();
		String line;
		

		// Read line by line from the input file.
		while ((line = myBuf.readLine()) != null) {
			// Parse the input line into JSON format
			Object obj = myParser.parse(line);
			JSONObject aTwit = (JSONObject) obj;

			// Get Entities key and the key hashtags
			JSONObject entity = (JSONObject) aTwit.get("entities");
			JSONArray hashtags = (JSONArray) entity.get("hashtags");

			String hashtagValue;

			if (hashtags.size() != 0) {
				String listHashtagsOfTwit = "";
				// output the hastags of 1 twit
				for (int i = 0; i < hashtags.size(); i++) {
					hashtagValue = ((JSONObject) hashtags.get(i)).get("text").toString();
					listHashtagsOfTwit += hashtagValue + " ";
				}
				if (arrayHashtags.indexOf(listHashtagsOfTwit) < 0) {
					counter++;
					// Store a new listHashtags to an array to check the
					// duplication
					arrayHashtags.add(listHashtagsOfTwit);

					// Write to file txt
					myWriter.write(listHashtagsOfTwit);
					myWriter.newLine();

				//	System.out.println(listHashtagsOfTwit);
				}
			}
		}
		myBuf.close();
		myWriter.close();
	}

	private static void buildGraph() throws IOException {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println("Start building graph");
		
		BufferedWriter myWriter = new BufferedWriter(new FileWriter(fileEdgeCalculation)); 
		
		int count = 0;
		int vertexCount = 0;
		SimpleWeightedGraph<String, DefaultWeightedEdge> hashtagGraph = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
	
		// Read the input file (each line is a list of hashtags we had in each Twit)
		List<String> lines = Files.readAllLines(Paths.get(fileTxtOutput), StandardCharsets.ISO_8859_1);
		for (int i = 0; i < lines.size(); i++) {
			
			// hashtagList contains hashtags of ONE Twit 
			List<String> hashtagList = Arrays.asList(lines.get(i).split(" "));
			
			// Replace the hashtag at idx i by its lower case
			for (int u = 0; u < hashtagList.size(); u++) {				
				hashtagList.set(u, hashtagList.get(u).toLowerCase());
			}
			
			// Add new (unique) Vertex to the Graph
			for (int u = 0; u < hashtagList.size(); u++){
				if (!hashtagGraph.containsVertex(hashtagList.get(u))){
					vertexCount++;
					hashtagGraph.addVertex(hashtagList.get(u));
				}					
			}
			
			// Create the edges from each pair of hashtags in ONE Twit
			if(hashtagList.size() > 2){
				for (int u = 0; u < hashtagList.size() - 1; u++){
					for (int v = u + 1; v < hashtagList.size(); v++) {
						// Make sure that 2 hashtags are different
						if(!hashtagList.get(u).equalsIgnoreCase(hashtagList.get(v))){
							// Check if the Graph has this edge or not
							if (!hashtagGraph.containsEdge(hashtagList.get(u),hashtagList.get(v))) {
								if (hashtagGraph.containsEdge(hashtagList.get(v), hashtagList.get(u))) {
									// Just to check if there is an error on duplication of A --> B and B --> A
									System.out.println("Directed edge detected **********************************");
								}									
								count++;
								hashtagGraph.addEdge(hashtagList.get(u), hashtagList.get(v));
								
								// Initialize the weight
								DefaultWeightedEdge edge = hashtagGraph.getEdge(hashtagList.get(u), hashtagList.get(v));
								double Weight = 1.0;						
								hashtagGraph.setEdgeWeight(edge, Weight);
							} 
							// Add 1 to weight if edge occurs 1 more time
							else {
								DefaultWeightedEdge edge = hashtagGraph.getEdge(hashtagList.get(u), hashtagList.get(v));
								double newWeight = hashtagGraph.getEdgeWeight(edge) + 1.0;						
								hashtagGraph.setEdgeWeight(edge, newWeight);
							}
						}						
					}
				}
			}							
		}
		
		String output = "";		
		for (DefaultWeightedEdge edge : hashtagGraph.edgeSet()) {
			if (hashtagGraph.getEdgeWeight(edge) > 30.0) {				
				// Write to the output file
				output = edge.toString() + " " + hashtagGraph.getEdgeWeight(edge);				
				myWriter.write(output);
				myWriter.newLine();
			}
		}
//		
		myWriter.close();
		
		System.out.println(vertexCount + " vertex");
		System.out.println(count + " edges");
		System.out.println("FINISHED");
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
	}
}
