# Tecnologias utilizadas

## Aplicativo (frontend)

| Tecnologia               | Versão      | Por que foi escolhida                                                             |
| ------------------------ | ----------- | --------------------------------------------------------------------------------- |
| Kotlin                   | 2.0.21      | Linguagem oficial do Android; null safety, `sealed` e extension functions         |
| Android Gradle Plugin    | 8.7.3       | Build do app (compileSdk 35, minSdk 26)                                           |
| Jetpack Compose (BOM)    | 2024.10.01  | Interface declarativa, sem XML de layout                                          |
| Material 3               | via BOM     | Componentes (`Scaffold`, `NavigationBar`, `AlertDialog`, `DatePicker`...) e temas |
| Navigation Compose       | 2.8.4       | `NavHost` com grafos aninhados e back stack                                       |
| Lifecycle / ViewModel    | 2.8.7       | ViewModels que sobrevivem à rotação e `collectAsStateWithLifecycle`               |
| Kotlin Coroutines / Flow | 1.9.0       | Assincronia e dados reativos                                                      |
| Room                     | 2.6.1       | Banco SQLite local com `Flow` (compilado com KSP)                                 |
| DataStore Preferences    | 1.1.1       | Sessão, tema e último filtro                                                      |
| Retrofit + OkHttp        | 2.11 / 4.12 | Cliente HTTP da API REST                                                          |
| kotlinx.serialization    | 1.7.3       | Conversão JSON                                                                    |
| Coil                     | 2.7.0       | Carregar a foto de perfil                                                         |
| JUnit                    | 4.13.2      | Testes unitários (filtros, validações, hash de senha)                             |

## Backend de desenvolvimento

| Tecnologia  | Versão | Uso                                                |
| ----------- | ------ | -------------------------------------------------- |
| Node.js     | LTS    | Executa o servidor                                 |
| json-server | 0.17.4 | API REST simulada (`/rides`) a partir de `db.json` |

## Ferramentas

| Ferramenta     | Uso                                                        |
| -------------- | ---------------------------------------------------------- |
| Android Studio | Desenvolvimento, emulador e Gradle                         |
| Git + GitHub   | Versionamento (Conventional Commits, branches por feature) |
| Prettier       | Formatação de JSON e Markdown                              |
