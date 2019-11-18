import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

class MyFS{

    //Variáveis a serem utilizadas posteriormentes em operações de IO
    static String man = "ls - listar diretório\nmkdir - criar diretório\nclear - limpa o shell\nexit - sair do shell\ninit - inicializa o filesytem\nload - carrega o filesystem\ndelfs - deleta o filesystem";
    static int block_size = 1024;
	static int blocks = 2048;
	static int fat_size = blocks * 2;
	static int fat_blocks = fat_size / block_size;
	static int root_block = fat_blocks;
	static int dir_entry_size = 32;
    static int dir_entries = block_size / dir_entry_size;
    static Set<String> operations = new HashSet<>();
    static String file = "filesystem.dat";

    /* FAT data structure */
	final static short[] fat = new short[blocks];
	/* data block */
	final static byte[] data_block = new byte[block_size];

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        FileSystem fs = new FileSystem();

        System.out.println("Bem vindo ao sistema de arquivos mais megaboga de todos!");

        System.out.println("Digite um dos comandos para realizar uma ação (man - comandos disponíveis): ");
        
        while(true){
            String op = sc.nextLine();
            parseAndExecute(op);
        }
    }

    static void parseAndExecute(String s){
        if(s.isEmpty()) {System.out.println("Invalid operation!"); return;}

        String [] split = s.split("\\s+");
        String op = split[0];

        if(!opExists(op)) {System.out.println("Invalid operation!"); return;}

        doOperation(split);
    }

    private static boolean opExists(String op){
        operations.add("man");operations.add("ls");operations.add("mkdir");operations.add("clear");operations.add("exit");operations.add("init");operations.add("load");operations.add("delfs");
        return operations.contains(op);
    }

    /**
     * @param args Array de string digitada pelo usuário que representa a operação a ser realizada.
     * Executa a operação e/ou imprime no shell alguma resposta.
     */
    private static void doOperation(String[] args){

        switch(args[0]){
            case "init":
                init(args);
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
            case "man":
                System.out.println(man);
                break;
            case "clear":
                System.out.print("\033[H\033[2J");  
                System.out.flush();  
                break;
            case "delfs":
                delfs(args);
                break;
            case "exit":
                System.exit(0);
            default:
                System.out.println("command not found");
                break;
        }

    }

    private static void init(String[] args){
        System.out.println(args.length);
        if(args.length > 1){
            System.out.println("Invalid arguments!");
            return;
        }
        System.out.println("Inicializando filesystem...");
        initFat();
        initEmptyRootBlock();
        System.out.println("Inicializacao concluida.");
    }

    private static void delfs(String[] args){
        if(args.length > 1){
            System.out.println("Invalid arguments!");
            return;
        }
        System.out.println("Removendo filesystem...");
        if(!deleteFs()) System.out.println("Erro ao remover filesystem."); else System.out.println("Remoção concluida.");
    }

    private static boolean deleteFs(){
        String fPath = checkIfExists();
        File f = new File(fPath);
        return f.delete();
    }

    private static void load(String[] args){
        if(args.length > 1){
            System.out.println("Invalid arguments!");
            return;
        }
        System.out.println("Carregando filesystem...");
        _load();
        return;
    }

    private static void _load(){
        String file = checkIfExists();
        if(file.isEmpty()){System.out.println("Não foi possível carregar a fat"); return;}
        short fromDisk[] = FileSystem.readFat(file);
        for (int i = 0; i < blocks; i++) {
            fat[i] = fromDisk[i];
        }
        System.out.println("Filesystem carregado!");
    }   

    private static void initFat(){
        /* initialize the FAT */
		for (int i = 0; i < fat_blocks; i++)
        fat[i] = 0x7ffe;
        fat[root_block] = 0x7fff;
        for (int i = root_block + 1; i < blocks; i++)
            fat[i] = 0;
        /* write it to disk */
        FileSystem.writeFat("filesystem.dat", fat);
    }

    private static void initEmptyRootBlock(){
        for (int i = 0; i < block_size; i++)
            data_block[i] = 0;
        FileSystem.writeBlock("filesystem.dat", root_block, data_block);

        /* write the remaining data blocks to disk */
		for (int i = root_block + 1; i < blocks; i++)
            FileSystem.writeBlock("filesystem.dat", i, data_block);
    }

    /**
     * Verifica se já existe um FS inicializado no diretório no qual o programa está rodando.
     * @return boolea true se existe, falso caso não exista
     */
    private static String checkIfExists(){
        final String dir = System.getProperty("user.dir");  
        File f = new File(dir+"/filesystem.dat");
        return f.exists() ? f.getAbsolutePath() : "";
    }


    private static void ls(String s){
        int block = getBlockFromPath(s, false);

        if(block == -1){
            System.err.println("Caminho informado não existe.");
            return;
        }

        System.err.println("Listagem da pasta: ");
        for (int i = 0; i < 32; i++) {
            System.out.println(i+1 + "\t" + new String(FileSystem.readDirEntry(block, i).filename));
        }
        
    }

    /**
     * esse cond é uma gambiarra pq to com sono e amanha a gente ajeita isso
     */
    private static int getBlockFromPath(String s, boolean cond){
        String[] path = s.split("\\/+");
        

        int size = path.length;
        if(cond == true) size = path.length -1;
        int block = root_block;
        DirEntry entry;
        entry = FileSystem.readDirEntry(block, 0);

        for (int i = 1; i < size; i++) {
            for (int j = 0; j < 32; j++) {
                entry = FileSystem.readDirEntry(block, j);

                if( new String(entry.filename).trim().equals(path[i])){
                    block = entry.first_block;
                    break;
                }
                if(j == 31) return -1;
            }
        }

        return block;
    }


    private static void mkdir(String path){
        int blockPrev = getBlockFromPath(path, true);
        int blockEmpty = getFirstEmptyBlock();
        int entry = getEntry(blockPrev);

        for(int i = 0; i < block_size; i++){
            data_block[i] = 0;
        }

        String[] file = path.split("\\/+");

        System.out.println("Path: " + path + " entry: " + entry + " blockPrev: " + blockPrev + " blockEmpty: " +  blockEmpty);
        DirEntry dir_entry = new DirEntry();
		String name = file[file.length-1];
		byte[] namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x02;
		dir_entry.first_block = (short)blockEmpty;
		dir_entry.size = 222; // ???? nao sei oq é
        FileSystem.writeDirEntry(blockPrev, entry, dir_entry, data_block);

        fat[blockEmpty] = 0x7fff; // só pra ver se muda
        FileSystem.writeFat("filesystem.dat", fat);

    }

    private static int getFirstEmptyBlock(){
        int block = -1;
        for (int i = 0; i < fat.length; i++) {
            if(fat[i] == 0){
                block = i;
                break;
            }
        }
        return block;
    }

    private static int getEntry(int block){
        int entry = -1;
        for (int i = 0; i < 32; i++) {
            String file = new String(FileSystem.readDirEntry(block, i).filename).trim();
           if(file.equals("")){
               entry = i;
               return entry;
           }
        }
        return entry;
    }


}