import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FilterInputStream;

public class FileSystem {
	static int block_size = 1024;
	static int blocks = 2048;
	static int fat_size = blocks * 2;
	static int fat_blocks = fat_size / block_size;
	static int root_block = fat_blocks;
	static int dir_entry_size = 32;
	static int dir_entries = block_size / dir_entry_size;

	/* reads a data block from disk */
	public static byte[] readBlock(String file, int block) {
		byte[] record = new byte[block_size];
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(block * block_size);
			fileStore.read(record, 0, block_size);
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return record;
	}

	/* writes a data block to disk */
	public static void writeBlock(String file, int block, byte[] record) {
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(block * block_size);
			fileStore.write(record, 0, block_size);
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* reads the FAT from disk */
	public static short[] readFat(String file) {
		short[] record = new short[blocks];
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(0);
			for (int i = 0; i < blocks; i++)
				record[i] = fileStore.readShort();
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return record;
	}

	/* writes the FAT to disk */
	public static void writeFat(String file, short[] fat) {
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(0);
			for (int i = 0; i < blocks; i++)
				fileStore.writeShort(fat[i]);
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* reads a directory entry from a directory */
	public static DirEntry readDirEntry(int block, int entry) {
		byte[] bytes = readBlock("filesystem.dat", block);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		DataInputStream in = new DataInputStream(bis);
		DirEntry dir_entry = new DirEntry();

		try {
			in.skipBytes(entry * dir_entry_size);

			for (int i = 0; i < 25; i++)
				dir_entry.filename[i] = in.readByte();
			dir_entry.attributes = in.readByte();
			dir_entry.first_block = in.readShort();
			dir_entry.size = in.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dir_entry;
	}

	/* writes a directory entry in a directory */
	public static void writeDirEntry(int block, int entry, DirEntry dir_entry, byte[] data_block) {
		byte[] bytes = readBlock("filesystem.dat", block);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		DataInputStream in = new DataInputStream(bis);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bos);

		try {
			for (int i = 0; i < entry * dir_entry_size; i++)
				out.writeByte(in.readByte());

			for (int i = 0; i < dir_entry_size; i++)
				in.readByte();

			for (int i = 0; i < 25; i++)
				out.writeByte(dir_entry.filename[i]);
			out.writeByte(dir_entry.attributes);
			out.writeShort(dir_entry.first_block);
			out.writeInt(dir_entry.size);

			for (int i = entry + 1; i < entry * dir_entry_size; i++)
				out.writeByte(in.readByte());
		} catch (IOException e) {
			e.printStackTrace();
		}

		byte[] bytes2 = bos.toByteArray();
		for (int i = 0; i < bytes2.length; i++)
			data_block[i] = bytes2[i];

		writeBlock("filesystem.dat", block, data_block);
	}
}
