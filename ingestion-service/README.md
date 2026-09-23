# IngestionServiceApp

## Overview

Parses and cleans `intersections-legacy.csv`, a messy legacy export of intersections, districts, and signal types data, and is the
first stop in the TrafficFlow pipeline. Independent Maven module, no parent pom.

Part of the [TrafficFlow](../README.md) project.

## Known data issues

`intersections-legacy.csv` is deliberately messy — cleaning it is the point of this service. Look
out for (and handle) at least:

- **Inconsistent casing** in IDs, names, and status/category values (`Active` /
  `active` / `ACTIVE`)
- **Padding** — leading/trailing spaces, and the occasional double space, inside
  fields
- **Duplicate records** for the same real-world entity, written with a different ID
  casing/format and/or slightly different field values
- **Inconsistent date formats** (`YYYY-MM-DD`, `MM/DD/YYYY`, `DD-MM-YYYY`, one- and
  two-digit months/days) and outright invalid dates
- **Missing / placeholder values** — blank fields, `N/A`, `n/a`, `TBD`, `unknown`,
  `-`, `NaN`
- **Invalid or non-numeric values** in numeric columns (negative counts, spelled-out
  numbers, unrealistic values)
- **Inconsistent boolean/flag representations** (`Y`/`N`, `yes`/`no`, `1`/`0`,
  `true`/`FALSE`)
- **Naming/spelling variants** for the same thing (e.g. regional spelling
  differences, synonyms)

## Worked example

A few raw rows from `intersections-legacy.csv`, and one reasonable cleaned shape for
them. Your field names/casing conventions don't need to match this exactly — the
point is normalizing consistently and handling the duplicate/missing cases, not
hitting this exact JSON.

Raw:

```csv
intersection_id,District ,signal_type,active_flag
INT-1001, Downtown ,4-way,Y
INT-1005,Downtown,Roundabout,true
int-1005,downtown ,ROUNDABOUT,TRUE
INT-1007,Eastside,,1
INT-1015,,4-way,Y
```

Cleaned:

```json
[
  { "id": "INT-1001", "district": "Downtown", "signalType": "4-way",       "active": true },
  { "id": "INT-1005", "district": "Downtown", "signalType": "roundabout",  "active": true },
  { "id": "INT-1007", "district": "Eastside", "signalType": null,          "active": true },
  { "id": "INT-1015", "district": null,       "signalType": "4-way",       "active": true }
]
```

What happened:
- `INT-1001`: trimmed the padded district (`" Downtown "` → `"Downtown"`); flag `Y` → `true`.
- `INT-1005` / `int-1005`: same real-world intersection under two ID casings and two
  signal-type casings — collapsed to a single record.
- `INT-1007`: signal type was blank in the source — kept as an explicit `null` rather
  than dropped or guessed, so downstream services can see it's missing.
- `INT-1015`: same idea for a missing district.

## Project structure

```
ingestion-service/
├── pom.xml
└── src/main/
    ├── java/co/wethinkcode/trafficflow/IngestionServiceApp.java
    └── resources/intersections-legacy.csv
```

## Build

```
mvn package
```

## Run

```
java -jar target/ingestion-service.jar
```

Listens on port `7020`.

## Endpoints

| Endpoint | Response |
|---|---|
| `GET /health` | `OK` |
| `GET /intersections` | `200` + JSON array of cleaned records |
| `GET /intersections/{id}` | `200` with the record, `404` if unknown (id lookup is case-insensitive) |

## Cleaning rules applied

- Whitespace: trims ends, collapses double spaces (`" Down  town "` → `"Down town"`→ title-cased)
- Casing: ids uppercased (`int-1005` → `INT-1005`), districts title-cased (`downtown` → `Downtown`), signal types lowercased (`4-Way` → `4-way`)
- Booleans: `Y`/`yes`/`1`/`true` → `true`, `N`/`no`/`0`/`false` → `false`
- Missing/placeholder (`blank`, `N/A`, `-`, `unknown`, `TBD`, `NaN`) → explicit JSON `null` — kept, never dropped or guessed
- Duplicates: rows with the same normalized id are collapsed, first non-null value per field wins

The cleaning logic lives in `IntersectionCleaner` (pure, unit-tested) and
`CsvIntersectionLoader` (CSV + de-duplication); `IngestionServiceApp` only wires
it to HTTP.

## Test

```
mvn test
```

To add real tests, add JUnit 5 + the Surefire plugin to `pom.xml`, put tests under
`src/test/java/co/wethinkcode/trafficflow/`, and run `mvn test`.
