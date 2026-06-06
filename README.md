# flip_iq_services

Spring Boot API for FlipIQ deal evaluation.

## Deal Evaluation Formula

FlipIQ evaluates fix-and-flip deals with the 70% rule:

```text
Maximum offer = after-repair value x 70% - rehab costs - holding costs - selling costs - profit buffer
```

The API also accepts purchase price and financing costs to calculate total project cost, projected profit, and offer spread.

## Run Locally

Run the API:

```bash
./gradlew bootRun
```

PostgreSQL is the only supported database for database-backed features. Start it with:

```bash
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

Evaluate a deal:

```bash
curl -X POST http://localhost:8080/api/deals/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "propertyAddress": "123 Main Street",
    "purchasePrice": 90000,
    "afterRepairValue": 250000,
    "rehabCosts": 35000,
    "financingCosts": 8000,
    "holdingCosts": 15000,
    "sellingCosts": 7000,
    "profitBuffer": 25000
  }'
```

Expected `maximumOffer`:

```json
93000.00
```

## Render

This repo includes `Dockerfile` and `render.yaml` for deploying the API to Render as a Docker web service.

Use `/actuator/health` as the health check path.
