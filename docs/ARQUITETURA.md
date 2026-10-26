# Arquitetura

O aplicativo segue **MVVM com camadas estritas** e fluxo de dados unidirecional (UDF).

```
UI Layer  →  ViewModel  →  Repository  →  Data Source (Room · DataStore · Retrofit)
```

## Camadas

| Camada      | Pacote                      | Responsabilidade                                                                                                            |
| ----------- | --------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| UI          | `ui/<módulo>`               | Telas Compose _stateless_ (`Screen`) e um `Route` que coleta o `StateFlow` da ViewModel                                     |
| ViewModel   | `ui/<módulo>/*ViewModel`    | Mantém o `UiState` (`StateFlow`), recebe eventos da UI, abre/fecha diálogos e chama os repositórios. Nunca recebe `Context` |
| Repository  | `data/repository`           | Regras de negócio e orquestração das fontes de dados. Retorna `Result<T>`                                                   |
| Data Source | `data/local`, `data/remote` | Room (entidades, DAOs), DataStore (preferências) e Retrofit (API REST)                                                      |
| Domínio     | `domain`                    | Modelos puros (`data class`, `enum`) e validações sem dependência de Android                                                |

## Estado da interface

Cada tela expõe um `UiState` modelado com `sealed interface`:

```kotlin
sealed interface RidesUiState {
    data object Loading : RidesUiState
    data class Error(val message: String) : RidesUiState
    data class Success(...) : RidesUiState
}
```

Eventos de uso único (Snackbar, voltar) saem por `SharedFlow<UiEvent>`.

## Persistência (Room)

| Tabela          | Conteúdo                                    |
| --------------- | ------------------------------------------- |
| `users`         | Conta, senha (hash + salt), vínculo, foto   |
| `rides`         | Caronas (locais e vindas da API) e `synced` |
| `ride_requests` | Solicitações de vaga e status               |
| `messages`      | Mensagens do chat por solicitação           |
| `contacts`      | Contatos de cada usuário                    |

O número de vagas confirmadas é calculado no SQL (`COUNT` de solicitações `CONFIRMED`), evitando coluna redundante. Confirmar uma vaga usa `withTransaction` para respeitar o limite de vagas.

## Sincronização offline-first

1. A UI sempre lê do Room (`Flow<List<T>>`).
2. `RideRepository.refresh()` envia as caronas com `synced = false` (`POST`/`PUT`) e depois baixa `GET /rides`.
3. Falhas de rede viram `Error` (tela cheia, se não há cache) ou Snackbar com "Tentar novamente".

## Navegação

- `AuthNavHost`: login e cadastro (exibido sem sessão).
- `MainNavHost`: barra inferior com 4 grafos aninhados — **Caronas**, **Atividade**, **Contatos** e **Perfil** — e telas internas (detalhe, formulário, chat).
- A sessão (DataStore) decide qual dos dois grafos aparece; logout volta ao login sem navegação manual.

## Injeção de dependências

`AppContainer` cria banco, DataStore, Retrofit e repositórios; `ViewModelFactories` monta cada ViewModel com os repositórios necessários. É o único ponto que conhece `Context`.
