# Pooja's Project

REST API assessment workspace (Set A6).

| Folder | What it is |
| --- | --- |
| [`restaurant-menu-api/`](restaurant-menu-api/) | Restaurant Menu REST API built with Spring Boot and in-memory storage |
| [`assessment-kit/`](assessment-kit/) | The kit used to check the API and send the result to the trainer |

## Quick start

Requires JDK 17+ and Maven for the API, and Node.js 18+ for the kit.

```bash
cd restaurant-menu-api
mvn spring-boot:run   # http://localhost:8080
mvn test
```

Then, with the API freshly started, check it with the kit:

```bash
cd assessment-kit
npm run evaluate
```

See [assessment-kit/README.md](assessment-kit/README.md) for details.
