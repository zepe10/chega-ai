# Backend (API REST de desenvolvimento)

Servidor [json-server](https://github.com/typicode/json-server) que expõe o recurso `rides` usado pelo aplicativo.

## Executar

```bash
npm install
npm start
```

A API fica em `http://localhost:3000/` (o emulador Android acessa pelo endereço `http://10.0.2.2:3000/`).

## Endpoints

| Método | Rota         | Descrição           |
| ------ | ------------ | ------------------- |
| GET    | `/rides`     | Lista as caronas    |
| POST   | `/rides`     | Cria uma carona     |
| PUT    | `/rides/:id` | Atualiza uma carona |
| DELETE | `/rides/:id` | Remove uma carona   |

Os dados iniciais estão em `db.json`.
