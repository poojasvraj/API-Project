# Restaurant Menu API

A REST API for restaurant dishes, built with **Spring Boot 3** (Java 17, Maven). Data is kept in memory, so the API starts empty every time it starts.

## Run

Requires JDK 17+ and Maven.

```bash
mvn spring-boot:run              # http://localhost:8080
PORT=3000 mvn spring-boot:run    # custom port
mvn test                         # automated tests
mvn package && java -jar target/restaurant-menu-api-1.0.0.jar
```

## Dish fields

| Field | Type | Rules |
| --- | --- | --- |
| `dishName` | string | 3–60 characters |
| `restaurant` | string | 2–40 characters |
| `category` | string | `Starter`, `MainCourse`, `Dessert`, `Beverage` or `Snack` |
| `price` | number | greater than 0, at most 5000 |
| `calories` | integer | 0–5000 |
| `isVegetarian` | boolean | |

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/dishes` | Create a dish (201) |
| `GET` | `/api/dishes` | List dishes (search, filter, sort, paginate) |
| `GET` | `/api/dishes/{id}` | Get one dish |
| `PUT` | `/api/dishes/{id}` | Replace a dish (all fields) |
| `PATCH` | `/api/dishes/{id}` | Update some fields |
| `DELETE` | `/api/dishes/{id}` | Delete a dish (204) |

Errors are JSON: `{"error": "..."}` with 400 (invalid input), 404 (unknown id or route) or 500.

### List query parameters

`search`, `category`, `isVegetarian`, `minPrice`, `maxPrice`, `sort` (`dishName`, `price`, `calories`), `order` (`asc`, `desc`), `page`, `limit` (max 50).
The response is `{ data, page, limit, total, totalPages }`.

## Examples

```bash
curl -i -X POST localhost:8080/api/dishes -H 'Content-Type: application/json' \
  -d '{"dishName":"Margherita","restaurant":"Luigi","category":"MainCourse","price":12.5,"calories":800,"isVegetarian":true}'

curl -i "localhost:8080/api/dishes?search=pizza&category=MainCourse&sort=price&order=desc&page=1&limit=10"

curl -i -X PATCH localhost:8080/api/dishes/1 -H 'Content-Type: application/json' -d '{"price":9.99}'

curl -i -X DELETE localhost:8080/api/dishes/1
```
