# GameTym — guia de apresentação

**António Pacheco | Formando n.º 02 | UC 00609**

## Demonstração sugerida (5 a 7 minutos)

1. Apresentar o objetivo: organizar uma coleção de videojogos num dispositivo Android, sem conta e com utilização offline.
2. Abrir a aplicação e mostrar a coleção. Uma instalação nova inclui dez jogos de exemplo.
3. Adicionar “Hades”, plataforma “PC”, género “Ação”, ano “2020”, estado “A Jogar” e favorito ativo.
4. Adicionar outro jogo com um estado diferente. Mostrar a pesquisa pelo nome e os filtros.
5. Abrir o Hades, alterar o estado para “Concluído” e guardar. Mostrar a atualização dos contadores.
6. Abrir as estatísticas e explicar o cálculo: jogos concluídos divididos pelo total, multiplicado por 100. Sem jogos, o resultado é 0%.
7. Alternar o modo claro/escuro. Fechar e reabrir a aplicação para mostrar a persistência dos dados e do tema.
8. Abrir um jogo, iniciar a eliminação e cancelar. Repetir e confirmar para demonstrar a diferença.
9. Mostrar um formulário vazio e tentar guardar para apresentar as mensagens de validação.

## Explicações do código

**Porque foi usado Kotlin?**

É um requisito do trabalho e permite definir o modelo de forma concisa com uma `data class`, representar os estados com um `enum` e trabalhar com Compose e corrotinas.

**O que é Jetpack Compose neste projeto?**

É a ferramenta que constrói a interface. As funções `@Composable` descrevem os ecrãs. Quando a lista ou os filtros mudam, o Compose atualiza as partes relevantes da interface.

**Onde estão guardados os dados?**

Na base de dados SQLite `gametym.db`, no espaço privado da aplicação. O `GameRepository` executa a leitura, inserção, atualização e eliminação. A escolha do tema fica em `SharedPreferences`.

**Como se distingue a criação da edição?**

Um jogo novo tem identificador 0 no modelo; o SQLite gera o identificador ao inserir. Um jogo existente tem identificador positivo e é atualizado pela sua chave primária. O formulário de edição começa preenchido com os valores existentes.

**Como são evitados dados inválidos?**

`GameValidation.errors` devolve os erros por campo. O formulário apresenta esses erros antes da gravação; o repositório também aplica a validação. Nome, plataforma e género são obrigatórios. O ano deve ser numérico e estar no intervalo permitido.

**Porque usar Dispatchers.IO?**

O acesso ao armazenamento pode demorar. Executar essas operações fora da tarefa da interface mantém os controlos responsivos. Depois da operação, a lista é relida e o estado da interface é atualizado.

**O que acontece ao eliminar?**

Primeiro aparece um diálogo de confirmação. Cancelar mantém o registo. Confirmar elimina apenas o jogo com o identificador escolhido e atualiza a coleção.

**Como funciona a pesquisa?**

A lista é filtrada pelo nome com correspondência parcial e sem distinguir maiúsculas. Os restantes filtros são combinados: o jogo tem de cumprir todas as condições selecionadas.

**Quais são as limitações?**

Existem capas locais para os dez jogos de exemplo; é possível escolher capas personalizadas; não existem exportação, cópias de segurança ou sincronização. Os nomes de plataforma e género são livres, pelo que diferenças de escrita podem criar filtros distintos. Limpar os dados ou desinstalar apaga a coleção. A validação foi feita no emulador disponível, sem cobertura de todos os dispositivos.

**O que melhorar numa próxima versão?**

Adicionar exportação/importação, listas normalizadas de plataformas e géneros, migrações de dados e separação do estado com ViewModel. Room pode simplificar a evolução da persistência.

## Antes da entrega

- Ler o relatório e executar os principais percursos da aplicação.
- Confirmar que o APK instala no dispositivo usado na apresentação.
- Entregar o ZIP com o projeto, o APK e o relatório PDF.
- Explicar as decisões com palavras próprias e adaptar a reflexão à experiência pessoal de aprendizagem.
