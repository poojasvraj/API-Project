# Restaurant Menu API (Assessment Set A6)

Express + in-memory storage (starts empty on every start). Port: `process.env.PORT || 8080`.

```bash
npm install
npm start            # http://localhost:8080
PORT=3000 npm start  # custom port
npm test             # 16 automated HTTP test groups
```

## curl cheatsheet
```bash
curl -i -X POST localhost:8080/api/dishes -H 'Content-Type: application/json' \
  -d '{"dishName":"Margherita","restaurant":"Luigi","category":"MainCourse","price":12.5,"calories":800,"isVegetarian":true}'
curl -i localhost:8080/api/dishes/<id>
curl -i "localhost:8080/api/dishes?search=pizza&category=MainCourse&isVegetarian=true&minPrice=5&maxPrice=20&sort=price&order=desc&page=1&limit=10"
curl -i -X PUT   localhost:8080/api/dishes/<id> -H 'Content-Type: application/json' -d '{...all 6 fields...}'
curl -i -X PATCH localhost:8080/api/dishes/<id> -H 'Content-Type: application/json' -d '{"price":9.99}'
curl -i -X DELETE localhost:8080/api/dishes/<id>
```

## Decision to check against your spec images
`limit` greater than 50 returns **400** (treated as an invalid value). To clamp to 50 instead,
change the `Number(q.limit) > MAX_LIMIT` line in `validation.js` to set `out.limit = MAX_LIMIT`.
