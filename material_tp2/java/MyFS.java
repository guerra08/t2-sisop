import java.io.File;
import java.util.Scanner;

class MyFS{

    //Variáveis a serem utilizadas posteriormentes em operações de IO
    public static String man = "ls - listar diretório\nmkdir - criar diretório\nclear - limpa o shell\nexit - sair do shell";
    static int block_size = 1024;
	static int blocks = 2048;
	static int fat_size = blocks * 2;
	static int fat_blocks = fat_size / block_size;
	static int root_block = fat_blocks;
	static int dir_entry_size = 32;
    static int dir_entries = block_size / dir_entry_size;
    
    /* FAT data structure */
	final static short[] fat = new short[blocks];
	/* data block */
	final static byte[] data_block = new byte[block_size];

    //TODO: Carregar a FAT / dados do disco.
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        FileSystem fs = new FileSystem();

        System.out.println("Bem vindo ao sistema de arquivos mais megaboga de todos!");

        String fCheck = checkIfExists();

        if(!fCheck.isEmpty()){
            System.out.println("Carregando FAT");
        }
        else{
            System.out.println("Inicializando FAT");
            initFat();
            initEmptyRootBlock();
        }

        System.out.println("Digite um dos comandos para realizar uma ação (man - comandos disponíveis): ");
        
        while(true){

            String op = sc.nextLine();
            doOperation(op);
        }
    }

    /**
     * @param op String digitada pelo usuário que representa a operação a ser realizada.
     * Executa a operação e/ou imprime no shell alguma resposta.
     */
    public static void doOperation(String op){

        switch(op){
            case "ls":
                System.out.println("list dir");
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
            case "exit":
                System.exit(0);
            default:
                System.out.println("command not found");
                break;
        }

    }

    public static void initFat(){
        /* initialize the FAT */
		for (int i = 0; i < fat_blocks; i++)
        fat[i] = 0x7ffe;
        fat[root_block] = 0x7fff;
        for (int i = root_block + 1; i < blocks; i++)
            fat[i] = 0;
        /* write it to disk */
        FileSystem.writeFat("filesystem.dat", fat);
    }

    public static void initEmptyRootBlock(){
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
    public static String checkIfExists(){
        final String dir = System.getProperty("user.dir");  
        File f = new File(dir+"/filesystem.dat");
        return f.exists() ? f.getAbsolutePath() : "";
    }

}