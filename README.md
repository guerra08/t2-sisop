# t2-sisop
<b>Trabalho 2 da cadeira de Sistemas Operacionais, 2019/2</b>

<b>Autores: Bruno Guerra e Eduardo Nohr Lessa</b>

Sistema de arquivos FAT e um simples shell:

O trabalho consiste na implementação de um simulador de um sistema de arquivos simples, baseado em tabela de alocação de 16 bits (FAT). Além disso, deve conter um shell onde são realizadas as operações no sistema de arquivos.

/material_tp2 --> Materiais iniciais diponibilizados pelo professor (códigos em C e Java).

/src          --> Código desenvolvido / utilizado para resolução do trabalho.

tp2.pdf       --> Enunciado do trabalho em formato PDF.

<b>Instruções de uso</b>

*Os comandos seguem o padrão: <b>operação argumento argumento</b>. Os argumentos são separados por espaço.*

- Compilar o arquivo MyFS.java, na pasta src: javac MyFS.java

- Executar o arquivo gerado: java MyFS

- Caso seja a primeira vez executando o programa, será necessário criar o filesystem. Para isso, digite o comando <b>init</b>

- Se tiver alguma dúvida sobre os comandos, digite man. Isso irá abrir o manual do shell.

- Após iniciar o sistema de arquivos, é necessário carregá-lo para a memória principal. Para isso, digite o comando <b>load.</b>

- Se você quiser criar um diretório, basta executar o comando <b>mkdir</b>. Por exemplo, para criar um diretório na raíz, digite /nome_dir ou /root/nome_dir.

- Para criar arquivos, use o comando <b>create</b>. Exemplo: create /nome_dir/nome_file

- Caso queria verificar os conteúdos de um diretório, utilize o comando <b>ls</b>. Exemplo: ls / ou ls /meu_dir

- Quando for escrever dados em um arquivo, utilize o comando <b>write</b>. A sua sintaxe segue o padrão: <b>write /caminho/arquivo "dados a serem escritos".</b>

- Para ler os conteúdos de um arquivo, digite o comando <b>read</b>. Como argumento, passa-se o caminho do arquivo. Exemplo: read /arquvo.
