/** 
 * Tad Edwards
 *	
 *	Name: RefactorCalculator.java
 *	Purpose: Delivers approximate size of soft code after refactoring code clones.
 *	Usage: ccfxRefactorSize [prettyPrint.tsv] [tokenfile.ccfxprep] [cloneM.tsv] [lineM.tsv] [sourceCodeFile]
 *
 */

import java.io.*;
import java.util.ArrayList;

import org.apache.commons.cli.ParseException;

public class RefactorCalculator {

public static void main(String[] args) {
		
		long startTime = System.currentTimeMillis(); //test scaling
		Engine engine = new Engine();

		try {
			engine.handleArguments(args);
		} catch (ParseException e) {
			System.out.println("Check your arguments and try again. Type -h for help.");
		} //handle args
		
		//input files
		String fileName1 = args[0];	//clone pairs
		String fileName2 = args[1]; //token file
		String fileName3 = args[2]; //clone metrics
		String fileName4 = args[3]; //line metrics
		String fileName5 = args[4]; //contains the actual source code: S
		
		//try read from file
		try {
			
			//Create matrix containing clone pairs information - CID FID SL EL
			int clonePairsRawLines = engine.countSectionLines(fileName1, "clone_pairs {", "}");
			String[] clonePairsRaw = engine.sectionToArray(fileName1, "clone_pairs {", "}");
			int[][] clonePairsList = new int[clonePairsRawLines][4];  
			clonePairsList = engine.toCloneMatrix(clonePairsRaw, clonePairsRawLines); //clonePairsList is the Matrix
			
			//initialize arrays containing token data and cloneMetrics, respectively
			String[] tokenFileArray = engine.fileToArray(fileName2);
			String[] cloneMetrics = engine.fileToArray(fileName3);
			
			//filter based on RNR, replace cloneMetrics[] with only lines of RNR less than .5
			cloneMetrics = engine.filterRNR(cloneMetrics); //possibly let user affect the filter for RNR
			int[][] matrixCID = engine.matrixCID(cloneMetrics); //start creating the matrix to store data
			clonePairsList = engine.filterClonePairs(clonePairsList, matrixCID); //then update clonePairsList matrix to only include unfiltered CIDs
			
			//add length and detect overlap to CID matrix, and update overlapMatrix
			engine.updateLength(clonePairsList, tokenFileArray, matrixCID); //adds the length to the CID matrix
			
			//get SLOC from lineM.tsv file
			String[] lineMetrics = engine.fileToArray(fileName4);
			int SLOC = Integer.valueOf((engine.toWord(lineMetrics[1])[2]));  
			
			//Handling of source code directly
			ArrayList<String> sourceCode = engine.fileToArrayList(fileName5);
			ArrayList<int[]> snippets = engine.snippetList(clonePairsList, tokenFileArray); //each int[] will be of size 4, containing <CID,SL,EL,Lenth>
			
			ArrayList<ArrayList<String>> attributes = engine.createAttributes(sourceCode, snippets);
			int[] lines = engine.cloneBeginnings(clonePairsList, tokenFileArray);
			ArrayList<String> refactorSourceOutline = engine.refactorSourceOutline(attributes, sourceCode, lines);
			ArrayList<ArrayList<ArrayList<String>>> listOfFID = engine.functionID(snippets, attributes, matrixCID);
			int[][] valuesChunk = engine.chunkValues(listOfFID);
			
			//get TCL
			int TCL = engine.totalCloneLines(attributes);
			
			//update matrixCID with containing values, chunk size, and chunk pieces
			engine.updateFinal(valuesChunk, matrixCID);
			
			//final calculation
			int refactoredSize = engine.approximateRefactoring(TCL, SLOC, valuesChunk, matrixCID);
			float PR = 100 - ((float)refactoredSize/(float)SLOC)*100;

			//Begin Output
			if(engine.ifDisplayVersion()){
				engine.displayVersion();
			}
			engine.mainOutput(matrixCID, valuesChunk, SLOC, TCL, refactoredSize, PR, fileName5);
			//Extra output arguments
			if(engine.ifPrime()){
				engine.outputSPrime(args[4], refactorSourceOutline); //Create file to store sPrime outline
			}
			if(engine.ifFunction()){
				engine.outputFunctionIDFile(args[4], listOfFID, matrixCID);
			}
			//Time
			if(engine.ifTime()){//used to test scaling 
				double time = System.currentTimeMillis() - startTime;
				time = time/1000.0;
				System.out.println("Execution time: " + time + " seconds");
			}
			
		} catch (IOException e1) {
			System.out.println("Error: make sure all files exist.");
			engine.printUsage();
		}
	
		System.exit(0);//Success	
	}
}

