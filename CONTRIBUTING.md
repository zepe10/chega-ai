# Guia de contribuição

## Fluxo de trabalho

1. Crie uma branch a partir da `main` com o padrão `tipo/descricao-curta`:
   - `feature/ride-list`
   - `fix/chat-scroll`
   - `docs/readme`
2. Faça commits pequenos e focados.
3. Abra um Pull Request (ou faça merge com `--no-ff` quando trabalhar localmente) para a `main`.

## Padrão de commits

Usamos [Conventional Commits](https://www.conventionalcommits.org/pt-br/): `tipo(escopo): descrição no imperativo`.

| Tipo       | Quando usar                                      |
| ---------- | ------------------------------------------------ |
| `feat`     | Nova funcionalidade                              |
| `fix`      | Correção de bug                                  |
| `refactor` | Mudança de código sem alterar comportamento      |
| `style`    | Formatação e visual (sem mudar lógica)           |
| `test`     | Testes                                           |
| `docs`     | Documentação                                     |
| `build`    | Gradle, dependências                             |
| `chore`    | Configuração do repositório e tarefas auxiliares |

Exemplos:

```
feat(rides): add ride list with search and filters
fix(chat): keep scroll at the latest message
docs: document architecture and technologies
```

## Estilo de código

- Kotlin com o estilo oficial (4 espaços) e sem comentários desnecessários.
- Arquivos JSON e Markdown formatados com Prettier (`.prettierrc.json`).
- Telas Compose _stateless_; lógica nas ViewModels; ViewModels sem `Context`.
