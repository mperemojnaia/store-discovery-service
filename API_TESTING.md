# Store Locator API — curl Test Guide

Start the service first:
```bash
# Default (Haversine)
./mvnw spring-boot:run

# With OpenRouteService (free key from https://openrouteservice.org/dev/#/signup)
DISTANCE_STRATEGY=ors ORS_API_KEY=your-key ./mvnw spring-boot:run

# With Google Distance Matrix API
DISTANCE_STRATEGY=google GOOGLE_MAPS_API_KEY=your-key ./mvnw spring-boot:run
```

## Happy Path

### Closest stores in Amsterdam
```bash
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041" | jq .
```

### Closest stores in Rotterdam
```bash
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=51.9225&longitude=4.4792" | jq .
```

### Closest stores in Eindhoven
```bash
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=51.4416&longitude=5.4697" | jq .
```

### With travel mode (affects ORS and Google strategies)
```bash
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041&travelMode=driving" | jq .
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041&travelMode=walking" | jq .
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041&travelMode=bicycling" | jq .
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041&travelMode=transit" | jq .
```

## ORS-Specific Tests (start with DISTANCE_STRATEGY=ors)

### Verify ORS is active (distanceType should be "ors")
```bash
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041" | jq .distanceType
```

### Driving distances (ORS profile: driving-car)
```bash
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041&travelMode=driving" | jq '.stores[] | {name: .addressName, distance}'
```

### Walking distances (ORS profile: foot-walking)
```bash
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041&travelMode=walking" | jq '.stores[] | {name: .addressName, distance}'
```

### Cycling distances (ORS profile: cycling-regular)
```bash
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041&travelMode=bicycling" | jq '.stores[] | {name: .addressName, distance}'
```

### Compare Haversine vs ORS driving distances side by side
```bash
echo "=== Haversine ===" && \
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041" | jq '.stores[] | {name: .addressName, distance}'
```
Then restart with `DISTANCE_STRATEGY=ors` and run:
```bash
echo "=== ORS Driving ===" && \
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041&travelMode=driving" | jq '.stores[] | {name: .addressName, distance}'
```

### Verify fallback (stop ORS or use invalid key, should fall back to haversine)
```bash
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041" | jq .distanceType
# Should show "haversine" if ORS is unreachable
```

### Boundary coordinates
```bash
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=0&longitude=0" | jq .
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=-90&longitude=-180" | jq .
curl -s "http://localhost:8080/api/v1/stores/closest?latitude=90&longitude=180" | jq .
```

## Error Cases (expect 400)

### Latitude out of range
```bash
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8080/api/v1/stores/closest?latitude=91&longitude=4.90" | jq .
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8080/api/v1/stores/closest?latitude=-91&longitude=4.90" | jq .
```

### Longitude out of range
```bash
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8080/api/v1/stores/closest?latitude=52.37&longitude=181" | jq .
```

### Missing required parameters
```bash
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8080/api/v1/stores/closest?latitude=52.37" | jq .
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8080/api/v1/stores/closest?longitude=4.90" | jq .
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8080/api/v1/stores/closest" | jq .
```

### Non-numeric coordinates
```bash
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8080/api/v1/stores/closest?latitude=abc&longitude=4.90" | jq .
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8080/api/v1/stores/closest?latitude=52.37&longitude=xyz" | jq .
```

### Invalid travel mode
```bash
curl -s -w "\nHTTP Status: %{http_code}\n" "http://localhost:8080/api/v1/stores/closest?latitude=52.37&longitude=4.90&travelMode=flying" | jq .
```

## Verbose (see headers)

```bash
curl -v "http://localhost:8080/api/v1/stores/closest?latitude=52.3676&longitude=4.9041"
```
