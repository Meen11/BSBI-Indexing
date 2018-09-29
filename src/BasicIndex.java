import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
/**Ravikan Thanapanaphruekkul 5988046
 * Pranpariya Sakarin 5988202
 * Pawin Sudjaidee 5988222
 * */

public class BasicIndex implements BaseIndex {

	@Override
	public PostingList readPosting(FileChannel fc) {
		/*
		 * TODO: Your code here
		 *       Read and return the postings list from the given file.
		 */
		int termID, docFreq;
		ByteBuffer buffer = ByteBuffer.allocate(4);
		try {
			if (fc.read(buffer)==-1) return null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		buffer.flip();
		termID = buffer.getInt();
		buffer = ByteBuffer.allocate(4);
		try {
			fc.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		buffer.flip();
		docFreq = buffer.getInt();

		ArrayList<Integer> postings = new ArrayList<>();
		for (int i=0; i<docFreq; i++){
			buffer = ByteBuffer.allocate(4);
			try {
				fc.read(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			buffer.flip();
			postings.add(buffer.getInt());
		}

//		System.out.println(termID+" : "+postings);


		return new PostingList(termID,postings);

	}

	@Override
	public void writePosting(FileChannel fc, PostingList p) {
		/*
		 * TODO: Your code here
		 *       Write the given postings list to the given file.
		 */
		ByteBuffer buffer = ByteBuffer.allocate((p.getList().size()+1+1)*4);
		int termID, docFreq;
		termID = p.getTermId();
		docFreq = p.getList().size();
		buffer.putInt(termID);
		buffer.putInt(docFreq);
		for (Integer docID: p.getList()){
			buffer.putInt(docID);
		}
		buffer.flip();
		while (buffer.hasRemaining()) {
			try {
				fc.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


}

