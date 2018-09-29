

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;

/**Ravikan Thanapanaphruekkul 5988046
 * Pranpariya Sakarin 5988202
 * Pawin Sudjaidee 5988222
 * */

public class Query {

	// Term id -> position in index file
	private  Map<Integer, Long> posDict = new TreeMap<Integer, Long>();
	// Term id -> document frequency
	private  Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();
	// Doc id -> doc name dictionary
	private  Map<Integer, String> docDict = new TreeMap<Integer, String>();
	// Term -> term id dictionary
	private  Map<String, Integer> termDict = new TreeMap<String, Integer>();
	// Index
	private  BaseIndex index = null;
	

	//indicate whether the query service is running or not
	private boolean running = false;
	private RandomAccessFile indexFile = null;
	
	/* 
	 * Read a posting list with a given termID from the file 
	 * You should seek to the file position of this specific
	 * posting list and read it back.
	 * */
	private  PostingList readPosting(FileChannel fc, int termId)
			throws IOException {
		/*
		 * TODO: Your code here
		 */
		if (posDict.containsKey(termId)){
			return index.readPosting(fc.position(posDict.get(termId)));
		}
		else {
			return null;
		}
	}
	
	
	public void runQueryService(String indexMode, String indexDirname) throws IOException
	{
		//Get the index reader
		try {
			Class<?> indexClass = Class.forName(indexMode+"Index");
			index = (BaseIndex) indexClass.newInstance();
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}
		
		//Get Index file
		File inputdir = new File(indexDirname);
		if (!inputdir.exists() || !inputdir.isDirectory()) {
			System.err.println("Invalid index directory: " + indexDirname);
			return;
		}
		
		/* Index file */
		indexFile = new RandomAccessFile(new File(indexDirname,
				"corpus.index"), "r");

		String line = null;
		/* Term dictionary */
		BufferedReader termReader = new BufferedReader(new FileReader(new File(
				indexDirname, "term.dict")));
		while ((line = termReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			termDict.put(tokens[0], Integer.parseInt(tokens[1]));
		}
		termReader.close();

		/* Doc dictionary */
		BufferedReader docReader = new BufferedReader(new FileReader(new File(
				indexDirname, "doc.dict")));
		while ((line = docReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
		}
		docReader.close();

		/* Posting dictionary */
		BufferedReader postReader = new BufferedReader(new FileReader(new File(
				indexDirname, "posting.dict")));
		while ((line = postReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
			freqDict.put(Integer.parseInt(tokens[0]),
					Integer.parseInt(tokens[2]));
		}
		postReader.close();
		
		this.running = true;
	}
    
	public List<Integer> retrieve(String query) throws IOException
	{	if(!running)
		{
			System.err.println("Error: Query service must be initiated");
		}

		/*
		 * TODO: Your code here
		 *       Perform query processing with the inverted index.
		 *       return the list of IDs of the documents that match the query
		 *
		 */

		ArrayList<Integer> list = new ArrayList<>();

		String[] tokens = query.trim().split("\\s+");

		for (String termID: termDict.keySet()){
			for (String token: tokens){
				if (token.equals(termID)){
					list.add(termDict.get(termID));				/**Adding the extracted termID to the arraylist*/
				}
			}
		}

		list = sortingArray(list);		/**Sorting the arraylist of termID*/

		if (list.isEmpty()) return null;	/**If there is no match word in termDict, it will return null*/

		FileChannel fc = indexFile.getChannel();
		HashSet<Integer> docID_set = new HashSet<>();

		ArrayList<PostingList> postingListArrayList = new ArrayList<>();

		for (Integer integer: list){
			postingListArrayList.add(readPosting(fc, integer));			/**Reading all the postinglist existing in the arraylist of termID from the file*/
		}

		if (postingListArrayList.size() == 1){					/**If there is just one postinglist, then return the docID list of it*/
			return  postingListArrayList.get(0).getList();
		}
		else {															/**If there is more than one arraylist, do a intersectizing*/
			HashSet<Integer> setofDOC = new HashSet<>();
			for (PostingList postingList: postingListArrayList){
				setofDOC.addAll(postingList.getList());
			}
			ArrayList<Integer> docIDs = new ArrayList<>(setofDOC);
			docIDs = sortingArray(docIDs);

			for (Integer docID: docIDs){
				int check = 1;
				for (PostingList postingList: postingListArrayList){
					if (!postingList.getList().contains(docID)) check = 0;
				}
				if (check==1) docID_set.add(docID);
			}
		}
		ArrayList<Integer> docID_list = new ArrayList<>(docID_set);
		docID_list = sortingArray(docID_list);							/**Sorting docID*/


		return docID_list;
	}
	
    String outputQueryResult(List<Integer> res) {
        /*
         * TODO: 
         * 
         * Take the list of documents ID and prepare the search results, sorted by lexicon order. 
         * 
         * E.g.
         * 	0/fine.txt
		 *	0/hello.txt
		 *	1/bye.txt
		 *	2/fine.txt
		 *	2/hello.txt
		 *
		 * If there no matched document, output:
		 * 
		 * no results found
		 * 
         * */

        if (res==null){
        	return "no results found";
		}else {
			StringBuilder output = new StringBuilder("");

			for (Integer docID : res) {
				for (Integer docID_dic : docDict.keySet()) {
					if (docID == docID_dic) {
						output.append(docDict.get(docID_dic) + "\n");		/**Appending all the filename into one single string*/
					}
				}
			}
			return output.toString();
		}
    }
	
	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 2) {
			System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
			return;
		}

		/* Get index */
		String className = null;
		try {
			className = args[0];
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get index directory */
		String input = args[1];
		
		Query queryService = new Query();
		queryService.runQueryService(className, input);
		
		/* Processing queries */
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		/* For each query */
		String line = null;
		while ((line = br.readLine()) != null) {
			List<Integer> hitDocs = queryService.retrieve(line);
			queryService.outputQueryResult(hitDocs);
		}
		
		br.close();
	}
	
	protected void finalize()
	{
		try {
			if(indexFile != null)indexFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<Integer> sortingArray(ArrayList<Integer> list){		/**The method for sorting*/

		int n = list.size();
		for(int i=0; i < n; i++){
			for(int j=1; j < (n-i); j++){
				if( list.get(j-1) > list.get(j) ){
					//swap elements
					Collections.swap(list, j-1, j);
				}
			}
		}
		return list;
	}
}

