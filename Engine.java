/**
 * @author Tad Edwards
 *	Name: Engine.java
 *	Purpose: Contains methods for use in RefactorCalculator.java
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.*;

public class Engine {
	
	private String version = "CCM Version 1.7.3";
	private String usage;
	private boolean calc = false, time = false, prime = false, function = false, largest = true, verbose = false, output = false, displayVersion = false;
	private float filterValue = .5f;
	private PrintStream out;
	private PrintStream oldOut = System.out;
	private Options options = new Options();
	private CommandLineParser parser = new DefaultParser();
	private HelpFormatter formatter = new HelpFormatter();
	private CommandLine cmd;
	private int numberOfCalls = 0; private int FIDCalls = 0; private int addBack = 0;
	
	Engine(){ //constructor
		initializeOptions();
	}
	
	void handleArguments(String[] args) throws ParseException{	
		cmd = parser.parse(options, args,false);
		if(cmd.hasOption("h")){ //help
			formatter.printHelp(usage,options,true);
			System.exit(0);
		}
		if(cmd.hasOption("o")){ //change output
			output = true;
		}
		if(cmd.hasOption("c")){ //calculations
			calc = true;
		}
		if(cmd.hasOption("ver")){ //version
			displayVersion = true;		
		}
		if(cmd.hasOption("t")){ //time
			time = true;
		}
		if(cmd.hasOption("p")){ //print sprime to file
			prime = true;
		}
		if(cmd.hasOption("f")){
			function = true;
		}
		if(cmd.hasOption("sm")){
			largest = false;
		}
		if(cmd.hasOption("rnr")){
			setRNRFilter();
		}
		if(cmd.hasOption("v")){
			verbose = true;
		}
		if(cmd.hasOption("mo")){
			function = true;
			verbose = true;
			prime = true;
			calc = true;
			time = true;
			output = true;
			displayVersion = true;
		}
		if(args.length < 5 && !cmd.hasOption("h")){ //help
			printUsage();
			System.exit(1);
		}
	}
	
	void initializeOptions(){
		usage = "RefactorCalculator [prettyPrint.tsv] [tokenfile.ccfxprep] [cloneM.tsv] [lineM.tsv]";
		options.addOption("h",false,"display help");
		options.addOption("c",false,"show calculations");
		options.addOption("ver",false,"display version");
		options.addOption("o",false,"output main data to .tsv file");
		options.addOption("t",false,"show runtime");
		options.addOption("p",false,"output s-prime file");
		options.addOption("f",false,"output FID list");
		options.addOption("sm",false,"results will assume smallest length from clone pairs");
		Option changeRNRFilter = Option.builder("rnr")
									.optionalArg(true)
									.argName("Filter Value")
									.hasArg(true)
									.desc("filters CID's with RNR of less than arg value")
									.build();
		options.addOption(changeRNRFilter);
		options.addOption("v",false,"changes output to verbose");
		options.addOption("mo",false,"produces maximum output (-f-v-p-c-t-ver-o)");
	}
	
	//called to manually set float value, if no argument, stays at default .5
	void setRNRFilter(){
		String argument = cmd.getOptionValue("rnr",".5");
		float valFloat = Float.parseFloat(argument);
		filterValue = valFloat;
	}

	//direct input of filename
	void setOutput(String filename) throws FileNotFoundException{
		System.out.println("Created file: " + filename);	
		try {
			out = new PrintStream(new FileOutputStream(filename));
			System.setOut(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	//resets Print stream to the console
	void resetOutput(){
		System.setOut(oldOut);
	}

	//prints the output either to console or to file depending on argument
	void mainOutput(int[][] matrixCID, int[][] valuesChunk, int SLOC, int TCL, int refactoredSize, float PR, String fileName){
		if(output){	
			String outputName = "Results-";
			if(verbose)
				outputName = "VerboseResults-";
			String[] temp = fileName.split("\\\\");
			temp = temp[temp.length-1].split("\\.nw");
			outputName += temp[0] + ".tsv";
			try {
				setOutput(outputName);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		if(verbose){
			//print out full table
			displayValues(matrixCID);
			System.out.println("--------------------------------------------------------------");
			System.out.println("Initial Size:\t\t" + SLOC + "\t|S|");
			System.out.println("Total Clone Length:\t" + TCL + "\tTCL");
			System.out.println("Refactored Size:\t" + refactoredSize + "\t|S'|");
			System.out.println("Percent Refactored:\t" + PR + "\tPR");
		}
		else{
			System.out.println("|S|\tTCL\t|S'|\tPR");
			System.out.println(SLOC + "\t" + TCL + "\t" + refactoredSize + "\t" + PR);
		}
		
		if(calc){
			System.out.println("--------------------------------------------------------------");
			showCalculation(valuesChunk, SLOC, TCL, matrixCID);
			System.out.println("--------------------------------------------------------------");
		}
		
		resetOutput();
	}
	
	/**
	 * prints readable string of the final calculation
	 * @param values - array of integers to be added together
	 * @param SLOC	- Source code size (without whitespace)
	 * @param CLOC - Total clone volume (without whitespace)
	 */
	void showCalculation(int[][] values, int SLOC, int CLOC, int[][] matrixCID){
		System.out.println("Calculation of |S'|:");
		System.out.println("|S'| = |S| - TLC + FC + MD + AB");
		System.out.println("|S'| = " + SLOC + " - " + CLOC + " + " + Integer.toString(numberOfCalls) + " + " + Integer.toString(FIDCalls*2) + " + " + Integer.toString(addBack));
		System.out.println("FC = " + numberOfCalls);
		System.out.println("MD = " + FIDCalls + " x 2");
		System.out.print("AB = ");
		for(int i = 0; i < matrixCID.length; i++){
			System.out.print(matrixCID[i][3]);
			if(i != matrixCID.length-1){
				System.out.print(" + ");
			}
		}
		System.out.println();
	}
	
	boolean ifTime(){
		return time;
	}
	
	boolean ifPrime(){
		return prime;
	}
	
	boolean ifFunction(){
		return function;
	}
		
	boolean ifDisplayVersion(){
		return displayVersion;
	}
	
	void displayVersion(){
		System.out.println(version);
		System.out.println("--------------------------------------------------------------");
	}
	
	//prints usage of program, error, and exits
	void printUsage(){
		System.out.println("Usage: RefactorCalculator [prettyPrint.tsv] [tokenfile.ccfxprep] [cloneM.tsv] [lineM.tsv]");
		System.out.println("Type -h for help.");
		System.exit(1); //error
	}
	
	//final calculation, returns integer value of result
	public int approximateRefactoring(int cloc, int sloc, int[][] values, int[][] matrixCID){
		int refactoredSize = sloc - cloc; //assume no clones
	
		//refactoredSize += addInts(values);
		for(int i = 0; i < values.length; i++){
			refactoredSize += matrixCID[i][3];
			refactoredSize += (values[i][1]*2); //multiply by 2 for calling and termination statement
			FIDCalls += values[i][1];
			addBack += matrixCID[i][3];
		}
		refactoredSize += numberOfCalls;
		
		return refactoredSize;
	}

	//Outputs function ID File
	public void outputFunctionIDFile(String sourceCodeName, ArrayList<ArrayList<ArrayList<String>>> functions, int[][] matrixCID){
		resetOutput();
		String outputName = "Functions-";
		String[] temp = sourceCodeName.split("\\\\");
		temp = temp[temp.length-1].split("\\.nw");
		outputName += temp[0] + ".txt";
		try {
			setOutput(outputName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		printFIDList(functions, matrixCID);
		resetOutput();
	}
	
	//Outputs sPrime file
	public void outputSPrime(String sourceCodeName, ArrayList<String> refactorSourceOutline){
		resetOutput();
		String sPrime = "sPrime-";
		String[] temp = sourceCodeName.split("\\\\");
		temp = temp[temp.length-1].split("\\.nw");
		sPrime += temp[0];
		try {
			setOutput(sPrime);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		for(int i = 0; i < refactorSourceOutline.size(); i++){
			System.out.println(refactorSourceOutline.get(i));
		}
		resetOutput();
	}
	
	//Prints listOfFID
	public void printFIDList(ArrayList<ArrayList<ArrayList<String>>> listOfFID, int[][] matrixCID){	
		for(int i = 0; i < listOfFID.size(); i++){
			System.out.println("CID\t: " + matrixCID[i][0]);
			System.out.println("CHUNKS\t: " + matrixCID[i][4]);
			System.out.println("SIZE\t: " + listOfFID.get(i).size());
			for(int j = 0; j < listOfFID.get(i).size(); j++){
				System.out.println(listOfFID.get(i).get(j));
			}
			System.out.println();
		}
	}
	
	//returns array of length 2, 1st element contains size of the chunk, 2nd element is number of pieces the chunk is contained in
	public int[][] chunkValues(ArrayList<ArrayList<ArrayList<String>>> listOfFID){
		int[] values = new int[listOfFID.size()];
		int[][] chunkMatrix = new int[values.length][2];
		for(int i = 0; i < values.length; i++){
			int[] chunkInfo = chunkSize(listOfFID.get(i), values.length);
			chunkMatrix[i] = chunkInfo;			
		}	
		return chunkMatrix;
	}
	
	//Using lists from listOfFID determine chunk sizes
	public int[] chunkSize(ArrayList<ArrayList<String>> listOfFIDList, int size){
		int[] chunkInfo = new int[2];
		int minLength = listOfFIDList.get(0).size();
		boolean isChunk = false;
		int numberOfChunks = 0;
		int chunkSize = 0;	
		for(int i = 0; i < listOfFIDList.size(); i++){ //determine minLength
			if(listOfFIDList.get(i).size() < minLength){
				minLength = listOfFIDList.get(i).size();
			}
		}	
		for(int i = 0; i < listOfFIDList.size(); i++){ //determine individual chunksizes and number of chunks
			int currentSize = listOfFIDList.get(i).size();
			if(currentSize == minLength){
				if(!isChunk){
					numberOfChunks++;
				}
				isChunk = true;
			}
			else{
				isChunk = false;
			}
			if(isChunk){
				chunkSize++;
			}
		}
		chunkInfo[0] = chunkSize;
		chunkInfo[1] = numberOfChunks;
		
		return chunkInfo;
	}
	
	//Returns a list of doubly nested ArrayList, one for each CID - likely not efficient, consider refactoring in future
	public ArrayList<ArrayList<ArrayList<String>>> functionID(ArrayList<int[]> snippets, ArrayList<ArrayList<String>> attributes, int[][] matrixCID){
		ArrayList<ArrayList<ArrayList<String>>> listOfListOfStrings = new ArrayList<ArrayList<ArrayList<String>>>();
		ArrayList<ArrayList<ArrayList<String>>> listOfFunctionIDList = new ArrayList<ArrayList<ArrayList<String>>>();
		ArrayList<ArrayList<String>> functionIDList = new ArrayList<ArrayList<String>>();;
		Set<String> hs = new HashSet<String>();	
		
		int startingSnippet = 0;
		for(int i = 0; i < matrixCID.length; i++){
			functionIDList = new ArrayList<ArrayList<String>>();

			int POP = matrixCID[i][1];
			int position = startingSnippet;
			int initialPos = position;
			int length = 0;
			int CIDLength = snippets.get(startingSnippet)[3];
			
			if(largest){ //assume max length of CID pairs
				for(int j = 0; j < POP; j++){	
					if(snippets.get(position)[3] > CIDLength){
						CIDLength = snippets.get(position)[3];
					}
					position++;
				}
				matrixCID[i][2] = CIDLength; //update length
			}
			else if(!largest){ //smallest length of CID pairs
				for(int j = 0; j < POP; j++){	
					if(snippets.get(position)[3] < CIDLength){
						CIDLength = snippets.get(position)[3];
					}
					position++;
				}
				matrixCID[i][2] = CIDLength; //update length
			}
			
			position = startingSnippet;
			
			for(int j = 0; j < POP; j++){//create a list containing a list of strings for each snippet's attributes
				int SL = snippets.get(position)[1];
				//int EL = snippets.get(position)[2];
				//length = EL - SL + 1;
				length = CIDLength;
				ArrayList<ArrayList<String>> temporaryList = new ArrayList<ArrayList<String>>(length);
				for(int k = 0; k < length; k++){//place each attribute of the corresponding snippets with same CID in a temporaryList
					temporaryList.add(k, attributes.get(SL-1));
					SL++;
				}
				listOfListOfStrings.add(temporaryList); 
				position++;
			}
			
			//System.out.println("Should be " + length + " it is " + listOfListOfStrings.get(initialPos).size()); //BREAKPOINT
			for(int a = 0; a < listOfListOfStrings.get(initialPos).size(); a++){ //create the union of corresponding snippets
				hs.clear();
				for(int j = initialPos; j < position; j++){ 
					hs.addAll(listOfListOfStrings.get(j).get(a)); //there is some bug here with indexoutofbounds that needs to be addressed, doesn't always occur
				}
				ArrayList<String> newList = combineLists(hs);
				functionIDList.add(newList);
			}		
			listOfFunctionIDList.add(functionIDList); //should be n number (from amount of CIDs) of ArrayList<ArrayList<Strings>> stored
			startingSnippet += POP;
		}

		//System.out.println(listOfFunctionIDList.size());
		return listOfFunctionIDList;
	}
	
	//combines the lists stored in set hs
	public ArrayList<String> combineLists(Set<String> hs){	
		ArrayList<String> newList = new ArrayList<String>(hs);
		String[] tempString = newList.toArray(new String[newList.size()]);
		int[] tempInt = new int[tempString.length];
		for(int i = 0; i < tempInt.length; i++){
			tempInt[i] = Integer.parseInt(tempString[i]);
		}
		Arrays.sort(tempInt);
		newList.clear();
		for(int i = 0; i < tempInt.length; i++){
			newList.add(String.valueOf(tempInt[i]));
		}
		return newList;
	}
	
	/**
	 * Creates an outline for refactoring source code
	 * @param attributes attributes for each line
	 * @param sourceCode original source code
	 * @return an outline of where to remove clone, also counts how many calls are made
	 */
 	public ArrayList<String> refactorSourceOutline(ArrayList<ArrayList<String>> attributes, ArrayList<String> sourceCode, int[] lines){
		ArrayList<String> sourceOutline = sourceCode;
		String[] currentAttribute = attributes.get(0).toArray(new String[attributes.get(0).size()]);

		for(int i = 0; i < sourceOutline.size(); i++){
			String[] temp = attributes.get(i).toArray(new String[attributes.get(i).size()]); //temporarily make into an array for comparison
			
			if(!Arrays.equals(currentAttribute, temp) && temp.length > 0){
				int line = i + 1;
				sourceOutline.set(i,"//Insert Calling Statement Line: " + line + "   \t" + String.valueOf(attributes.get(i)));
				currentAttribute = temp;
				numberOfCalls++;
			}
			else if(!Arrays.equals(currentAttribute,temp)){
				currentAttribute = temp;
			}
			else if(Arrays.equals(currentAttribute, temp) && temp.length > 0){
				sourceOutline.set(i,"//<remove>");
			}			
		}
		
		
		//before removing, go back and make sure all function calls exist, increase number of calls if it didn't exist previously
		for(int i = 0; i < lines.length; i++){
			
			int line = lines[i] - 1;	
			String temp = sourceOutline.get(line);
			
			if((temp.length() < 33)){
				sourceOutline.set(line, "//Insert Calling Statement Line: " + line + "   \t" + String.valueOf(attributes.get(line)));
				numberOfCalls++;
			}
		}
		sourceOutline.removeAll(Collections.singleton("//<remove>")); //deletes all "//<remove>" comments
		return sourceOutline;
	}
 	
 	//stores the values of the starting line of each clone iteration
 	public int[] cloneBeginnings(int[][] clonePairsList, String[] tokenFileArray){
 		int[] lines = new int[clonePairsList.length];
 		for(int i = 0; i < lines.length; i++){
 			int position = clonePairsList[i][2] + 1;
 			String[] temp = tokenFileArray[position].split("\\.");
 			lines[i] = hexToDec(temp[0]);
 		}
 		return lines;
 	}
	
	/**
	 * Creates array list of of snippets (L)
	 * @param clonePairsList - matrix containing clone SL and EL
	 * @param tokenArray - array containing token information
	 * @return ArrayList containing all L, 3 tuples <CID,SL,EL>, NO DUPLICATES, ordered by CID
	 */
	public ArrayList<int[]> snippetList(int[][] clonePairsList, String[] tokenArray){
		ArrayList<int[]> snippets = new ArrayList<int[]>();
		Set<String> hs = new HashSet<>();
		ArrayList<String> snippetsTemp = new ArrayList<String>();
		
		for(int i = 0; i < clonePairsList.length; i++){ //gets 3 tuples, CID SL EL, places into set of Strings (to avoid duplicates)
			int CID = clonePairsList[i][0];
			String startToken = tokenArray[clonePairsList[i][2]]; //SL Token
			String endToken = tokenArray[clonePairsList[i][3]-1]; //EL Token 
			String[] tempStartArray = startToken.split("\\.");
			String[] tempEndArray = endToken.split("\\.");
			int SL = hexToDec(tempStartArray[0]); //Starting line
			int EL = hexToDec(tempEndArray[0]); //Ending line
			int length = EL - SL + 1;
			hs.add(CID+" "+SL+" "+EL+" "+length);
		}
		
		snippetsTemp.addAll(hs); //add all elements of hs to snippetsTemp so we can parse proper int values
	
		for(int j = 0; j < snippetsTemp.size(); j++){ //parse the int values, place into arrayList<int[]> 
			String[] temp = snippetsTemp.get(j).split("\\ ");
			int CID = Integer.parseInt(temp[0]), SL = Integer.parseInt(temp[1]), EL = Integer.parseInt(temp[2]), length = Integer.parseInt(temp[3]);
			int[] tempL = {CID,SL,EL,length};
			snippets.add(tempL);
		}
		
		Collections.sort(snippets, new Comparator<int[]>() {  //orders snippets list according to CID of arrays
		    public int compare(int[] a, int[] b) {
		    	return Integer.valueOf(a[0]).compareTo(Integer.valueOf(b[0]));
		    }
		});
		
		return snippets;
	}
	
	/**
	 * @param sourceCode The ArrayList<String> containing the source code
	 * @param snippets ArrayList<ints[]> containing snippets (CID, SL, EL)
	 * @return ArrayList<ArrayList<String>>
	 */
	public ArrayList<ArrayList<String>> createAttributes(ArrayList<String> sourceCode, ArrayList<int[]> snippets){
		ArrayList<ArrayList<String>> attributes = new ArrayList<ArrayList<String>>(sourceCode.size());
		for(int i = 0; i < sourceCode.size(); i++){ //initialize attributes
			attributes.add(i, new ArrayList<String>());
		}
		for(int i = 0; i < snippets.size(); i++){ //update attributes
			String att = String.valueOf(snippets.get(i)[0]); //CID (attribute)
			int start = snippets.get(i)[1]-1; //SL
			int end = snippets.get(i)[2]; //EL
			while(start < end){
				attributes.get(start).add(att);
				start++;
			}
		}
		return attributes;
	}
	
	/**
	 * @param overlapMatrix
	 * @param matrixCID
	 * @return a string of elements concerning what overlaps are contained in which methods
	 */
	public String[] reportAddBack(int[][] overlapMatrix, int[][] matrixCID){
		String[] value = new String[overlapMatrix.length];
		
		for(int i = 0; i < overlapMatrix.length; i++){
			value[i] = "";
			value[i] += Integer.toString(matrixCID[i][0]) + "-";
			value[i] += findAddBack(overlapMatrix, i, matrixCID);
		}
		
		return value;
	}
	
	//recursively determine what CID to substract from other CID
	public static String findAddBack(int[][] overlapMatrix, int position, int[][] matrixCID){
		String value = "";
		int pos = position; 
		for(int i = 0; i < overlapMatrix.length; i++){
			if(overlapMatrix[position][i] == 1){
				pos = i;
				value += matrixCID[pos][0] + ".";
				value += findAddBack(overlapMatrix,pos,matrixCID);
			}
		}
		return value;
	}
	
	/**
	 * Converts integers stored in valueArray into single values
	 * @param valueArray array containing strings with value information
	 * @param matrixCID	contains information about the CID, POP, LENGTH, OVERLAP
	 * @return converted value (integer)
	 */
	public int convertValues(int[] valueArray, int[][] matrixCID){
		int value = cidToLength(valueArray[0], matrixCID);
		for(int i = 1; i < valueArray.length; i++){
			if(valueArray[i] != 0){
				value -= cidToLength(valueArray[i], matrixCID); //removes the CID lengths the code clone contains
			}
		}
		return value;
	}
	
	/**
	 * Finds and returns corresponding length of clone according to CID
	 * @param CID - CID value
	 * @param matrixCID - matrix containing information about the CID such as POP, LENGTH, OVERLAP
	 * @return	length of the CID
	 */
	public int cidToLength(int CID, int[][] matrixCID){
		int length = 0;
		int position = CID;
		
		for(int i = 0; i < matrixCID.length; i++){
			if(matrixCID[i][0] == position){
				length = matrixCID[i][2]; //grab the length
			}
		}
		
		return length;
	}
		
	//deletes any duplicates after sorting, returns sorted array of what is left
	public int[] sortDelete(String[] tempContains, int length){
		ArrayList<String> arrayList = new ArrayList<String>(Arrays.asList(tempContains));//temporarily convert String to arrayList
		ArrayList<String> remove = new ArrayList<String>(); //to contain what needs to be removed
		for(int i = 0; i < arrayList.size()-1; i++){ //find what needs to be removed
			if(arrayList.get(i).equalsIgnoreCase(arrayList.get(i+1))){
				remove.add(arrayList.get(i));
			}
		}
		for(int i = 0; i < remove.size(); i++){ //remove it
			while(arrayList.remove(remove.get(i)));
		}
		int[] sortedArray = new int[arrayList.size()]; //make new array to return
		for(int i = 0; i < sortedArray.length; i++){
			sortedArray[i] = Integer.parseInt(arrayList.get(i));
		}
		return sortedArray;
	}
	
	/**
	 * @param intArray
	 * @return sum of all int values in an int array
	 */
	public int addInts(int[] intArray){
		int sum = 0;
		for(int i = 0; i < intArray.length; i++){
			sum += intArray[i];
		}
		return sum;
	}

	/**
	 * @param hexString - string of hex values
	 * @return integer of the converted hex value
	 */
	public int hexToDec(String hexString){
		int integerVal = Integer.parseInt(hexString, 16);
		return integerVal;
	}
	
	/**
	 * @param sortedValues - values of which methods are contained where
	 * @param valuesFID - contains chunk size and chunk pieces
	 * @param matrixCID - updates matrix CID to include these new values
	 */
	void updateFinal(int[][] valuesFID, int[][] matrixCID){
		for(int i = 0; i < matrixCID.length; i++){
			matrixCID[i][3] = valuesFID[i][0]; //chunk size
			matrixCID[i][4] = valuesFID[i][1]; //chunk pieces
		}
	}
	
	/**	
	 * Updates matrixCID with length information, which was initially blank
	 * @param dataMatrix - clone pairs information
	 * @param tokenFileArray 
	 * @param updatedMatrix - matrixCID
	 */
	void updateLength(int[][] dataMatrix, String[] tokenFileArray, int[][] updatedMatrix){
		for(int i = 0; i < dataMatrix.length; i++){
			int lengthOfClone = getCloneLength(dataMatrix[i][2], dataMatrix[i][3], tokenFileArray); //getLength
			addLength(updatedMatrix, dataMatrix[i][0], lengthOfClone); //add the length
		}
	}
	
	/**
	 * Adds length of clone to CID matrix
	 * @param matrix - matrix to update (matrixCID)
	 * @param CID - CID to be updated
	 * @param lengthOfClone - clone length
	 */
	void addLength(int[][] matrix, int CID,  int lengthOfClone){
		for(int i = 0; i < matrix.length; i++){
			if(matrix[i][0] == CID){ //if the CID matches, update CID with clone length 
				matrix[i][2] = lengthOfClone;
			}
		}
	}
	
	/**
	 * Calculates length of the clone based on the token file
	 * @param bg - starting point
	 * @param end - ending point
	 * @param tokenArray - tokenFileArray
	 * @return length of the clone
	 */
	public int getCloneLength(int bg, int end, String[] tokenArray){
		int length = 0; //start from zero
		int lineNum = -1; //-1 to be sure first check has no match
		int tempLineNum; //stores value we are checking
		
		for(int arrayPos = bg; arrayPos < end; arrayPos++){
			String temp = tokenArray[arrayPos];
			String[] tempArray = temp.split("\\.");
			tempLineNum = hexToDec(tempArray[0]); //stores line number
			if(lineNum != tempLineNum){	//if the linesNums aren't the same
				lineNum = tempLineNum; 	//update lineNum to current line
				length++;				//increase the length by 1
			}
		}	
		return length;
	}
	
	/**
	 * Places clone information into an integer matrix so info can easily be accessed
	 * @param array - Contains clone information
	 * @return integer matrix with clone information
	 */
	public int[][] matrixCID(String[] array){
		int[][] matrix = new int[array.length-1][5];
		for(int i = 1; i < array.length; i++){
			String[] words = toWord(array[i]);	
			matrix[i-1][0] = Integer.valueOf(words[0]); //CID
			matrix[i-1][1] = Integer.valueOf(words[2]); //POP
			matrix[i-1][2] = 0; //default length
			matrix[i-1][3] = 0; //default chunk size
			matrix[i-1][4] = 0; //default chunk pieces
		}
		return matrix;
	}

	/**
	 * Split array into a more accessible matrix
	 * @param array - 
	 * @param length - length of the array
	 * @return Matrix containing CID FID SL EL
	 */
	public int[][] toCloneMatrix(String[] array, int length){
		int[][] matrix = new int[length][4];
		for(int i = 0; i < length; i++){		
			for(int j = 0; j < 6; j++){	
				if(j == 0){
					matrix[i][j] = Integer.valueOf(toWord(array[i])[0]); //matrix[i][0] = array[i][0] CLONE ID
				}
				else if(j == 1){
					String temp = toWord(array[i])[1];
					String[] tempArray = temp.split("\\.");
					matrix[i][j] = Integer.valueOf(tempArray[0]); //matrix[i][1] = array[i][1] FILE ID
				}
				else if(j == 2){
					String temp = toWord(array[i])[1];
					String[] tempArray = temp.split("\\.");
					temp = tempArray[1]; //bg-end
					tempArray = temp.split("\\-");
					matrix[i][j] = Integer.valueOf(tempArray[0]); //matrix[i][2] = array[i][2] SL
				}
				else if(j == 3){
					String temp = toWord(array[i])[1];
					String[] tempArray = temp.split("\\.");
					temp = tempArray[1]; //bg-end
					tempArray = temp.split("\\-");
					matrix[i][j] = Integer.valueOf(tempArray[1]); //matrix[i][3] = array[i][3] EL
				}
			}
		}
		return matrix;
	}
	
	//Using the attributes arraylist, count number of lines containing clones in the source code
	public int totalCloneLines(ArrayList<ArrayList<String>> attributes){
		int totalCloneLines = 0;
		for(int i = 0; i < attributes.size(); i++){
			if(!attributes.get(i).isEmpty()){
				totalCloneLines++;
			}
		}
		return totalCloneLines;
	}
	
	/**
	 * Filters out CIDs based on a filter
	 * @param cloneMetricArray - original unfiltered array
	 * @param filter - floating point value used to filter RNR
	 * @return array containing clone metrics minus clones filtered out due to low RNR
	 */
	public String[] filterRNR(String[] cloneMetricArray){
		ArrayList<String> filteredList = new ArrayList<String>(); //place into string array list because we dont know the size
		filteredList.add("CID     LEN     POP     NIF     RAD     RNR     TKS     LOOP    COND    McCabe");
		for(int i = 1; i < cloneMetricArray.length; i++){
			String[] temp = toWord(cloneMetricArray[i]);
			float RNR = Float.parseFloat(temp[5]);			
			if(RNR > filterValue){
				filteredList.add(cloneMetricArray[i]);
			}
				
		}
		String[] filteredArray = filteredList.toArray(new String[filteredList.size()]); //convert Arraylist to array
		return filteredArray;
	}
	
	//removes duplicate clone pairs
	public int[][] filterClonePairs(int[][] clonePairsList, int[][] matrixCID){
		ArrayList<int[]> filteredPairs = new ArrayList<int[]>();
		for(int i = 0; i < clonePairsList.length; i++){
			for(int j = 0; j < matrixCID.length; j++){
				if(clonePairsList[i][0] == matrixCID[j][0]){
					filteredPairs.add(clonePairsList[i]);
				}
			}
		}
		int[][] filteredClonePairs = filteredPairs.toArray(new int[filteredPairs.size()][4]);
		return filteredClonePairs;
	}
	
	/**
	 * Splits an input string into separate words and places them into an array
	 * @param line - a String, ex: "Hello world"
	 * @return String array, ex: {"Hello","world"}
	 */
	public String[] toWord(String line){
		String[] words = line.split("\\s+");
		return words;
	}
	
	/**
	 * @param path
	 * @param begin
	 * @param end
	 * @return String array, each element different line from the specified section
	 * @throws IOException
	 */
	public String[] sectionToArray(String path, String begin, String end) throws IOException {
		int lines = countSectionLines(path, begin, end);
		String[] array = sectionToArray(path,begin,end,lines);
		return array;
	}
		
	/**
	 * @param path - file to open
	 * @param begin - which token to begin reading input
	 * @param end - which token to stop reading input
	 * @param length - length of the section (in lines)
	 * @return String array, each element different line from the specified section
	 * @throws IOException
	 */
	private String[] sectionToArray(String path, String begin, String end, int length) throws IOException {
		String[] array = new String[length];
		FileInputStream fstream = new FileInputStream(path);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine;
		int i = 0;
		//read through all lines of the file, get only information about clone pairs
		//add each line to array
		while((strLine = br.readLine()) != null){	
			if(strLine.equals(begin)){	
				while((strLine = br.readLine()) != null){
					if(strLine.equals(end)){
						break;
					}
					array[i] = strLine;
					i++;
				}
			}			
		}
		fstream.close();
		return array;
	}
	
	/**
	 * @param path - file to open
	 * @param begin - which token to begin reading input
	 * @param end - which token to stop reading input
	 * @return number of lines in the section
	 * @throws IOException
	 */
	public int countSectionLines(String path, String begin, String end) throws IOException {	
		int lines = 0;	
		FileInputStream fstream = new FileInputStream(path);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine;
		//read through all lines of the file, get only information about clone pairs
		while((strLine = br.readLine()) != null){	
			if(strLine.equals(begin)){	
				while((strLine = br.readLine()) != null){
					if(strLine.equals(end)){
						break;
					}
					lines++;
				}
			}			
		}
		fstream.close();
		return lines;
	}
	
	/**
	 * @param path - file to open
	 * @return An array, each element containing a string from the corresponding line in the file that was opened
	 * @throws IOException
	 */
	public String[] fileToArray(String path) throws IOException{
		int lines = countLines(path);
		String file[] = new String[lines];
		FileInputStream fstream = new FileInputStream(path);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine;
		int i = 0;
		while((strLine = br.readLine()) != null){
			file[i] = strLine;
			i++;
		}
		fstream.close();
		return file;
	}

	/**
	 * @param path - file to open
	 * @return Array list, each element containing a line from the input file
	 * @throws IOException
	 */
	public ArrayList<String> fileToArrayList(String path) throws IOException{
		int lines = countLines(path);
		ArrayList<String> file = new ArrayList<String>(lines);
		FileInputStream fstream = new FileInputStream(path);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine;
		while((strLine = br.readLine()) != null){
			file.add(strLine);
		}
		fstream.close();
		return file;
	}
	
	/**
	 * @param path - file to open
	 * @return number of total lines a file contains
	 * @throws IOException
	 */
	public int countLines(String path) throws IOException{
		int lines = 0;
		FileInputStream fstream = new FileInputStream(path);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		@SuppressWarnings("unused")
		String strLine;
		//read through all lines of the file, get only information about clone pairs
		while((strLine = br.readLine()) != null){
			lines++;			
		}		
		fstream.close();
		return lines;
	}
		
	//prints contents of integer matrix
	void printMatrix(int[][] matrix){
		for(int i = 0; i < matrix.length; i++){
			for(int j = 0; j < matrix[i].length; j++){
				System.out.print(matrix[i][j] + "\t");
			}
			System.out.println("");
		}
	}
	
	/**
	 * Display primary values to user in readable format: CID	POP	LENGTH	OVERLAP	CONTAINS
	 * @param matrixCID - matrix that contains CID, POP, LENGTH, and OVERLAP
	 * @param sortedValues - matrix containing CIDs that are contained within the first CID
	 * @see	sortedValues[i][0] = containing CID, sortedValues[i][1] = contained CID
	 */
	void displayValues(int[][] matrixCID){
		System.out.println("CID\tPOP\tLENGTH\tCSIZE\tCHUNKS");
		for(int i = 0; i < matrixCID.length; i++){
			for(int j = 0; j < matrixCID[i].length; j++){
				System.out.print(matrixCID[i][j]+"\t");
			}
			System.out.println();
		}	
	}
	
}
