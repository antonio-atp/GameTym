# GameTym

Aplicação Android de gestão de uma coleção pessoal de videojogos, desenvolvida para a UC 00609 em Kotlin e Jetpack Compose.

## Abrir e executar

1. Abrir esta pasta no Android Studio e aguardar a sincronização Gradle.
2. Usar o JDK integrado no Android Studio, versão 17 ou superior, e instalar o SDK Android 37 e Build Tools solicitados pelo Gradle.
3. Selecionar um emulador ou telemóvel com Android 8.0 (API 26) ou superior e carregar em Run.

O projeto inclui Gradle Wrapper 9.5.0. As primeiras compilações podem precisar de Internet para obter dependências. `local.properties` é específico deste computador e não integra a distribuição.

No Windows, com JAVA_HOME configurado para o JDK do Android Studio:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

O APK é gerado em `app/build/outputs/apk/debug/app-debug.apk`. A cópia para entrega está em `output/GameTym.apk`. É um APK de demonstração assinado com a chave de debug; não é uma publicação na Play Store.

## Utilizar

- Tocar em **Adicionar jogo**, preencher nome, plataforma, género, ano e estado, e guardar.
- No formulário, usar **Escolher imagem** para selecionar uma capa do dispositivo; também é possível alterar ou remover a imagem ao editar. A aplicação guarda o acesso à imagem escolhida, pelo que esta deve continuar disponível no dispositivo.
- Tocar num cartão para editar ou eliminar. A eliminação pede confirmação.
- Pesquisar pelo nome, combinar filtros de estado, plataforma, género e favoritos, e escolher a ordenação.
- Tocar na estrela para marcar favoritos e abrir **Estatísticas** para acompanhar a coleção.
- Usar **Claro/Escuro** para mudar o tema. A preferência fica guardada.

A instalação começa com dez jogos de exemplo e respetivas capas locais. As imagens do relatório usam dados de demonstração criados exclusivamente no emulador pelos testes. As capas dos dez jogos de exemplo estão incluídas na aplicação; os restantes cartões apresentam a imagem escolhida ou as iniciais.

## Organização

- `data/Game.kt`: modelo, estados e validação.
- `data/GameRepository.kt`: leitura, inserção, atualização e eliminação em SQLite.
- `ui/GameTymApp.kt`: coleção, formulário, estatísticas e navegação.
- `MainActivity.kt`: entrada da aplicação.
- `src/test`: testes da validação.
- `src/androidTest`: testes de persistência, navegação e fluxo de edição/eliminação; capturas reais.
- `output`: APK, relatório, evidências e pacote para entrega.

SQLite mantém os dados no armazenamento privado da aplicação. Fechar e voltar a abrir a aplicação não elimina a coleção. Desinstalar a aplicação ou limpar os seus dados elimina a coleção; esta versão não inclui exportação nem sincronização.

## Regras de validação

Nome obrigatório (1–100 caracteres); plataforma e género obrigatórios (1–60 caracteres); ano numérico entre 1950 e o ano atual mais cinco, permitindo lançamentos anunciados. Os espaços exteriores são removidos. Os estados possíveis são Por Jogar, A Jogar e Concluído.

## Entrega e apresentação

Consultar o relatório PDF para a análise e as evidências. A capa identifica António Pacheco, formando n.º 02, conforme indicado pelo utilizador. Antes da entrega, rever o relatório e preparar a explicação do código e das escolhas técnicas.

## Referências técnicas

As capas dos dez exemplos foram obtidas das páginas públicas dos respetivos jogos na Steam e são incluídas para este projeto académico. Os direitos das imagens pertencem aos respetivos titulares. As capas são associadas pelo nome; ao alterar o nome de um jogo, o cartão pode voltar a apresentar as iniciais.

- https://developer.android.com/develop/ui/compose
- https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper
- https://developer.android.com/build/migrate-to-built-in-kotlin
- https://developer.android.com/training/testing/instrumented-tests
