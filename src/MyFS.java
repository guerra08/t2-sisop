import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

class MyFS {

    // Variáveis a serem utilizadas posteriormentes em operações de IO
    static String man = "ls - list directory\nmkdir - create directory\nclear - cleans the shell\nexit - exits the shell\ninit - initializes the filesytem\nload - loads the filesystem\ndelfs - deletes the filesystem";
    static int block_size = 1024;
    static int blocks = 2048;
    static int fat_size = blocks * 2;
    static int fat_blocks = fat_size / block_size;
    static int root_block = fat_blocks;
    static int dir_entry_size = 32;
    static int dir_entries = block_size / dir_entry_size;
    static Set<String> operations = new HashSet<>(
            Arrays.asList("man", "ls", "mkdir", "clear", "exit", "init", "load", "delfs", "create", "unlink", "write", "read","append"));
    static String file = "filesystem.dat";

    /* FAT data structure */
    final static short[] fat = new short[blocks];
    /* data block */
    final static byte[] data_block = new byte[block_size];

    /* Estrutura do data_block utilizada nos métodos */
    static byte[] d_block;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Welcome to the Filesystem Safado 2019");

        System.out.println("Input a command (type man for available commands): ");

        while (true) {
            String op = sc.nextLine();
            int s = parseAndExecute(op);
            if (s == 2) {
                sc.close();
                System.out.println("Falooooou!");
                System.exit(0);
            }
        }
    }

    /**
     * Recebe uma String, realiza o seu parse e caso seja válida, verifica qual
     * comando executar.
     * 
     * @param s String com a operação do shell (init, mkdir /dir)
     */
    private static int parseAndExecute(String s) {

        if (s.isEmpty()) {
            System.err.println("Invalid operation.");
            return 1;
        }

        String[] split = s.split("\\s+");
        String op = split[0];

        if (!opExists(op)) {
            System.err.println("Invalid operation.");
            return 1;
        }

        try {
            if (op.equals("write") || op.equals("append")) {
                String[] text = Arrays.copyOfRange(split, 2, split.length);
                String toAdd = String.join(" ", text);
                toAdd = toAdd.replace("\"", "");
                return doOperation(split, toAdd);
            }
        } catch (Exception e) {
            System.err.println("Invalid operation.");
        }

        return doOperation(split);
    }

    /**
     * Verifica se a operação existe
     * 
     * @param op
     * @return boolean
     */
    private static boolean opExists(String op) {
        return operations.contains(op);
    }

    private static int doOperation(String[] args, String toAdd) {
        System.out.println(toAdd);
        try {
            switch (args[0]) {
            case "write":
                write(args[1], toAdd, true);
                break;
            case "append":
                append(args[1], toAdd);
                break;
            }
            return 0;
        } catch (Exception e) {
            System.out.println(e);
            System.err.println("Failed to execute command. Possible wrong or missing arguments.");
            return 1;
        }
    }

    /**
     * @param args Array de string digitada pelo usuário que representa a operação a
     *             ser realizada. Executa a operação e/ou imprime no shell alguma
     *             resposta.
     */
    private static int doOperation(String[] args) {

        try {
            switch (args[0]) {
            case "init":
                init(args);
                load(args);
                break;
            case "load":
                load(args);
                break;
            case "ls":
                ls(args[1]);
                break;
            case "mkdir":
                mkdir(args[1]);
                break;
            case "create":
                create(args[1]);
                break;
            case "man":
                System.out.println(man);
                break;
            case "clear":
                if (System.getProperty("os.name").contains("Windows"))
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                else {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                }
                break;
            case "delfs":
                delfs(args);
                break;
            case "unlink":
                unlink(args[1]);
                break;
            case "read":
                read(args[1]);
                break;
            case "exit":
                return 2;
            default:
                System.err.println("command not found");
                break;
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Failed to execute command. Possible wrong or missing arguments.");
            return 1;
        }

    }

    /**
     * Inicializa o sistema de arquivos (filesystem.dat)
     * 
     * @param args Argumentos / comandos digitados
     */
    private static void init(String[] args) {
        if (args.length > 1) {
            System.out.println("Invalid arguments.");
            return;
        }
        System.out.println("Initializing filesystem...");
        initFat();
        initEmptyRootBlock();
        System.out.println("Done.");
    }

    private static void delfs(String[] args) {
        if (args.length > 1) {
            System.err.println("Invalid arguments!");
            return;
        }
        System.out.println("Removing filesystem...");
        if (!deleteFs())
            System.err.println("Error removing filesystem.");
        else
            System.out.println("Done.");
    }

    private static boolean deleteFs() {
        String fPath = checkIfExists();
        File f = new File(fPath);
        return f.delete();
    }

    private static void load(String[] args) {
        if (args.length > 1) {
            System.err.println("Invalid arguments.");
            return;
        }
        System.out.println("Loading filesystem...");
        _load();
        return;
    }

    private static void _load() {
        String file = checkIfExists();
        if (file.isEmpty()) {
            System.out.println("Unable to load filesystem.");
            return;
        }
        short fromDisk[] = FileSystem.readFat(file);
        for (int i = 0; i < blocks; i++) {
            fat[i] = fromDisk[i];
        }
        System.out.println("Done.");
    }

    private static void initFat() {
        /* initialize the FAT */
        for (int i = 0; i < fat_blocks; i++)
            fat[i] = 0x7ffe;
        fat[root_block] = 0x7fff;
        for (int i = root_block + 1; i < blocks; i++)
            fat[i] = 0;
        /* write it to disk */
        FileSystem.writeFat("filesystem.dat", fat);
    }

    private static void initEmptyRootBlock() {
        for (int i = 0; i < block_size; i++)
            data_block[i] = 0;
        FileSystem.writeBlock("filesystem.dat", root_block, data_block);

        /* write the remaining data blocks to disk */
        for (int i = root_block + 1; i < blocks; i++)
            FileSystem.writeBlock("filesystem.dat", i, data_block);
    }

    /**
     * Verifica se já existe um FS inicializado no diretório no qual o programa está
     * rodando.
     * 
     * @return boolea true se existe, falso caso não exista
     */
    private static String checkIfExists() {
        final String dir = System.getProperty("user.dir");
        File f = new File(dir + "/filesystem.dat");
        return f.exists() ? f.getAbsolutePath() : "";
    }

    private static void ls(String s) {
        int block = getBlockFromPath(s, false);
        int blockPrev = getBlockFromPath(s, true);

        if (block == -1) {
            System.err.println("Path does not exist.");
            return;
        }

        String[] pathSplited = s.split("\\/+");

        int j = 0;

        while(j<32){
            DirEntry de = FileSystem.readDirEntry(blockPrev, j);
            if(pathSplited.length != 0 &&  new String(de.filename).trim().equals(pathSplited[pathSplited.length-1])){ //Found
                if(de.attributes != 0x02){
                    System.err.println("Trying to use ls on non directory.");
                    return;
                }
                else{
                    break;
                }
            }
            j++;
        }
        

        System.err.println("Index\tName\t\tType\t\tSize");
        for (int i = 0; i < 32; i++) {
            DirEntry entry = FileSystem.readDirEntry(block, i);
            String nameFile = new String(entry.filename).trim();
            if (!nameFile.equals("")) {
                short att = entry.attributes;
                int size = entry.size;
                String type;
                type = (att == 1) ? "File" : "Dir";
                System.out.println(i + "\t" + nameFile + "\t\t" + type + "\t\t" + size);
            }
        }

    }

    private static int getBlockFromPath(String path, boolean cond) {
        String[] pathSplited = path.split("\\/+");

        int size = pathSplited.length;
        if (cond == true)
            size = pathSplited.length - 1;
        int block = root_block;
        DirEntry entry;
        entry = FileSystem.readDirEntry(block, 0);

        for (int i = 1; i < size; i++) {
            for (int j = 0; j < 32; j++) {
                entry = FileSystem.readDirEntry(block, j);

                if (new String(entry.filename).trim().equals(pathSplited[i])) {
                    block = entry.first_block;
                    break;
                }
                if (j == 31)
                    return -1;
            }
        }

        return block;
    }

    private static void mkdir(String path) {
        int blockPrev = getBlockFromPath(path, true);
        int blockEmpty = getFirstEmptyBlock();
        int entry = getEntry(blockPrev);
        String[] file = path.split("\\/+");

        if(file[file.length - 1].length() > 25){
            System.err.println("Name is larger than 25 characters.");
            return;
        }

        String local = file.length == 2 ? "root" : file[file.length - 2];
        System.out.println("Directory " + file[file.length - 1] + " created with succeess on directory " + local);

        DirEntry dir_entry = createEntry(file[file.length - 1], 0x02, blockEmpty);

        d_block = FileSystem.readBlock("filesystem.dat", blockPrev);

        FileSystem.writeDirEntry(blockPrev, entry, dir_entry, d_block);

        fat[blockEmpty] = 0x7fff;
        FileSystem.writeFat("filesystem.dat", fat);

    }

    private static void create(String path) {
        
        int blockPrev = getBlockFromPath(path, true);
        int blockEmpty = getFirstEmptyBlock();
        int entry = getEntry(blockPrev);
        String[] file = path.split("\\/+");

        if(file[file.length - 1].length() > 25){
            System.err.println("Name is larger than 25 characters.");
            return;
        }

        String local = file.length == 2 ? "root" : file[file.length - 2];

        System.out.println("File " + file[file.length - 1] + " created with succeess on directory " + local);
        DirEntry dir_entry = createEntry(file[file.length - 1], 0x01, blockEmpty);

        d_block = FileSystem.readBlock("filesystem.dat", blockPrev);

        FileSystem.writeDirEntry(blockPrev, entry, dir_entry, d_block);

        fat[blockEmpty] = 0x7fff;
        FileSystem.writeFat("filesystem.dat", fat);
    }

    private static void append(String path, String toAdd){
        write(path, toAdd, false);
    }

    private static void write(String path, String toAdd, boolean overwrite) {
        int blockArq = getBlockFromPath(path, false);
        if(overwrite){
            cleanFatAndBlocks(blockArq);
        }
        int numBlocks = (int) Math.ceil(toAdd.length() / 1024);
        byte[] fileBytes = toAdd.getBytes();
        byte[] block = new byte[1024];
        int k = 0;
        if(!overwrite){
            block = FileSystem.readBlock("filesystem.dat", blockArq);
            int pos;
            if(fat[blockArq] == 0x7fff){
                block = FileSystem.readBlock("filesystem.dat", blockArq);
            }
            else{
                pos = fat[blockArq];
                while(pos != 0x7fff){
                    block = FileSystem.readBlock("filesystem.dat", pos);
                    pos = fat[pos];
                    blockArq = fat[pos]; 
                }
            }
            for (int i = 0; i < block.length; i++) {
                if(block[i] == 0) {
                    k = i; 
                    break;
                }
            }
        }

        for(int i = 0; i < fileBytes.length; i++){
            block[k++] = fileBytes[i];
            if(k == 1024){
                FileSystem.writeBlock("filesystem.dat", blockArq, block);
                if(numBlocks > 1){
                    int aux = blockArq;
                    fat[blockArq] = 0x7fff;
                    FileSystem.writeFat("filesystem.dat", fat);
                    
                    blockArq = getFirstEmptyBlock();    
                    fat[aux] = (short)blockArq;
                    fat[blockArq] = 0x7fff;
                }
                else{
                    fat[blockArq] = 0x7fff;
                    FileSystem.writeFat("filesystem.dat", fat);
                }
                k = 0;
                for (int j = 0; j < 1024; j++) {
                    block[j] = 0;
                }
            }

            if(k < 1024 && i == fileBytes.length-1){
                for(int c = k+1; c < 1024; c++){
                    block[c] = 0;
                }
                    fat[blockArq] = 0x7fff;
                    FileSystem.writeBlock("filesystem.dat", blockArq, block);
                    FileSystem.writeFat("filesystem.dat", fat);

            }
        }

    }

    private static void cleanFatAndBlocks(int block){
        
        byte[] blockByte = new byte[1024];
        short pos = fat[block];
        fat[block] = 0x0000;
        FileSystem.writeBlock("filesystem.dat", block, blockByte);

        while(pos != 0x7fff){
            short aux = fat[pos];
            fat[pos] = 0x0000;
            FileSystem.writeBlock("filesystem.dat", pos, blockByte);
            pos = aux;
        }

        FileSystem.writeFat("filesystem.dat", fat);
    }

    private static DirEntry createEntry(String name, int type, int firstBlock) {
        DirEntry dir_entry = new DirEntry();
        byte[] namebytes = name.getBytes();
        for (int i = 0; i < namebytes.length; i++)
            dir_entry.filename[i] = namebytes[i];
        dir_entry.attributes = (byte) type;
        dir_entry.first_block = (short) firstBlock;
        dir_entry.size = 0;

        return dir_entry;
    }

    private static void unlink(String path) {
        String[] file = path.split("\\/+");

        int blockPrev = getBlockFromPath(path, true);
        int blockToUnlik = getBlockFromPath(path, false);
        int entry = -1;

        DirEntry dir = new DirEntry();
        DirEntry toUse;

        for (int i = 0; i < 32; i++) {
            dir = FileSystem.readDirEntry(blockPrev, i);
            String fileName = new String(dir.filename).trim();

            if (fileName.equals(file[file.length - 1])) {
                entry = i;
                break;
            }
        }

        if(entry == -1){
            System.err.println("Entry not found.");
            return;
        }


        DirEntry check = FileSystem.readDirEntry(blockPrev, entry);
        if(check.attributes == 0x02){ // É um diretório
            for (int i = 0; i < 32; i++) {
                DirEntry entryAux = FileSystem.readDirEntry(blockToUnlik, i);
                if(entryAux.attributes != 0x00){
                    System.err.println("Can't delete directory with entries.");
                    return;
                }
            }
        }

        System.out.println("Successfully removed");

        cleanFatAndBlocks(blockToUnlik);
        toUse = createEntry("", 0x00, 0);
        d_block = FileSystem.readBlock("filesystem.dat", blockPrev);
        FileSystem.writeDirEntry(blockPrev, entry, toUse, d_block);
    }

    private static void read(String path){
        int blockArq = getBlockFromPath(path, false);
        int blockPrev = getBlockFromPath(path, true);

        String[] pathSplited = path.split("\\/+");

        if(pathSplited.length == 0){
            System.err.println("Trying to use read on non file.");
            return;
        }

        int j = 0;

        while(j<32){
            DirEntry de = FileSystem.readDirEntry(blockPrev, j);
            if(pathSplited.length != 0 &&  new String(de.filename).trim().equals(pathSplited[pathSplited.length-1])){ //Found
                if(de.attributes != 0x01){
                    System.err.println("Trying to use read on non file.");
                    return;
                }
                else{
                    break;
                }
            }
            j++;
        }

        String mensagem;

        byte[] record;
        record = FileSystem.readBlock("filesystem.dat", blockArq);

        mensagem = new String(record);

        short pos = fat[blockArq];
        while(pos != 0x7fff){
            record = FileSystem.readBlock("filesystem.dat", pos);
            pos = fat[pos]; 

            mensagem = mensagem + new String(record);
        }

    }

    private static int getFirstEmptyBlock() {
        int block = -1;
        for (int i = 0; i < fat.length; i++) {
            if (fat[i] == 0) {
                block = i;
                break;
            }
        }
        return block;
    }

    private static int getEntry(int block) {
        int entry = -1;
        for (int i = 0; i < 32; i++) {
            String file = new String(FileSystem.readDirEntry(block, i).filename).trim();
            if (file.equals("")) {
                entry = i;
                return entry;
            }
        }
        return entry;
    }

}