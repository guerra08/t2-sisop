public class DirEntry {
	byte[] filename = new byte[25];
	byte attributes;
	short first_block;
	int size;

	@Override
	public String toString(){
		return "Attributes: " + attributes + " First block: " + first_block + " Size: " + size;
	}
}
