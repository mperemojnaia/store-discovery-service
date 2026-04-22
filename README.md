# Closest Stores Service

A Spring Boot microservice that finds the 5(default) closest Jumbo stores to a given geographic position. Supports distance calculation via the Haversine formula (default), OpenRouteService Matrix API, or the Google Distance Matrix API.

## Prerequisites

- **Local development:** Java 21
- **Containerized:** Docker

## Build & Run

### With Maven Wrapper

```bash
# Run directly
./mvnw spring-boot:run

# Build JAR
./mvnw clean package

# Run the JAR
java -jar target/closest-stores-*.jar
```

### With Docker

```bash
# Build the image
docker build -t closest-stores .

# Run with defaults (Haversine distance, port 8080)
docker run -p 8080:8080 closest-stores

# Run with ORS or Google — use an env file for API keys (never pass secrets inline)
docker run -p 8080:8080 --env-file .env closest-stores
```

Create a `.env` file in the project root (normally we would keep the api-key in the secrets and not commit it) with the following content:
Currently 'ors' is set in .env as default.
```env
DISTANCE_STRATEGY=ors
ORS_API_KEY=your-ors-key-here
# DISTANCE_STRATEGY=google
# GOOGLE_MAPS_API_KEY=your-google-key-here
```

## API Documentation

Swagger UI is available when the application is running:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI spec (JSON): [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## API Usage

### Find closest stores

```
GET /api/v1/stores/closest?latitude=52.3676&longitude=4.9041
```

With travel mode (applies when using `ors` or `google` distance strategy):

```
GET /api/v1/stores/closest?latitude=52.3676&longitude=4.9041&travelMode=driving
```

Valid travel modes: `driving`, `walking`, `bicycling`, `transit`

With custom result limit (default: 5, max: 50):

```
GET /api/v1/stores/closest?latitude=52.3676&longitude=4.9041&limit=10
```

### Example response

```json
{
  "stores": [
    {
      "addressName": "Jumbo Amsterdam Eerste Oosterparkstraat",
      "city": "Amsterdam",
      "postalCode": "1091 GZ",
      "street": "Eerste Oosterparkstraat",
      "latitude": 52.3612,
      "longitude": 4.9133,
      "distance": 0.87,
      "uuid": "...",
      "complexNumber": "...",
      "showWarningMessage": true,
      "todayOpen": "08:00",
      "todayClose": "22:00",
      "locationType": "Supermarkt",
      "collectionPoint": false,
      "sapStoreID": "..."
    }
  ],
  "distanceType": "haversine"
}
```

## Distance Strategies

| Strategy | How it works | Pros | Cons |
|---|---|---|---|
| `haversine` | Straight-line (as-the-crow-flies) distance using the Haversine formula | Fast, no API key needed, zero cost | Doesn't account for roads, rivers, or terrain |
| `ors` | Real travel distance via [OpenRouteService](https://openrouteservice.org) Directions API | Accurate road distances, free API key, supports walking/cycling | Rate-limited, slower (sequential HTTP calls per candidate) |
| `google` | Real travel distance via Google Distance Matrix API | Most accurate, supports transit | Requires paid Google Cloud account, not currently active |

All strategies fall back to Haversine automatically if the external API fails (circuit breaker + retry via Resilience4j).

## Configuration

| Variable | Description | Default |
|---|---|---|
| `DISTANCE_STRATEGY` | Distance calculation method (`haversine`, `ors`, or `google`) | `haversine` |
| `ORS_API_KEY` | OpenRouteService API key (required when strategy is `ors`). Free at [openrouteservice.org](https://openrouteservice.org) | — |
| `GOOGLE_MAPS_API_KEY` | Google Maps API key (required when strategy is `google`) | — |
| `SERVER_PORT` | HTTP server port | `8080` |
| `STORE_MAX_RESULTS` | Default number of stores returned | `5` |
| `STORE_GEOHASH_PRECISION` | Geohash cell precision for spatial pre-filtering (higher = smaller cells) | `4` |

> **Note on Google Distance Matrix API:** The `google` distance strategy is implemented but not currently active. We don't have a Google Cloud account with the Distance Matrix API enabled yet. When a Google Cloud project is set up in the future, add the API key to the `.env` file and set `DISTANCE_STRATEGY=google`. Until then, use `haversine` (default) or `ors`.

## Testing

Run the test suite:

```bash
./mvnw clean verify
```

For manual API testing with curl examples (Haversine, ORS, Google, error cases), see [API_TESTING.md](API_TESTING.md).
