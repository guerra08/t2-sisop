import java.io.File;
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
        operations.add("add3");operations.add("findInRoot");
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
                System.out.println("make dir");
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
            case "add3":
                add3ToRoot();
                break;
            case "findInRoot":
                findInRoot(args[1]);
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
        System.out.println(args.length);
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

    private static void add3ToRoot() {
        DirEntry dir_entry = new DirEntry();
		String name = "file1";
		byte[] namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 1111;
		dir_entry.size = 222;
		FileSystem.writeDirEntry(root_block, 0, dir_entry);

		name = "file2";
		namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 2222;
		dir_entry.size = 333;
		FileSystem.writeDirEntry(root_block, 1, dir_entry);

		name = "file3";
		namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 3333;
		dir_entry.size = 444;
		FileSystem.writeDirEntry(root_block, 2, dir_entry);
    }

    private static void findInRoot(String s){
        DirEntry dir_entry;
        boolean found = false;
        for (int i = 0; i < dir_entries; i++) {
            dir_entry = FileSystem.readDirEntry(root_block, i);
            String cur = new String(dir_entry.filename).trim();
			if(s.equals(cur)){
                found = true;
                System.out.println("Encontrou! " + cur);
                break;
            }
        }
        if(!found) System.err.println("Nao encontrou!");
    }

    private static void ls(String s){
        
    }

    private static int parsePathString(String s){
        String[] path = s.split("\\/+");
        int block = root_block;
        int length = path.length;
        DirEntry de = FileSystem.readDirEntry(root_block, 0);
        for (int i = 0; i < length; i++) {
           //Terminar 
           //Percorrer as 32 entradas do diretório até achar uma com o nome igual
           //Se não encontra, já retorna erro (não encontrou)
        }
        return 0;
    }

}