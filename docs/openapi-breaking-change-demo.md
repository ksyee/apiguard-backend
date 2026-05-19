# OpenAPI Breaking Change Demo

This demo shows the contract-change feature with two small OpenAPI documents.

## Files

- `docs/demo/openapi/baseline-openapi.json`: initial API contract
- `docs/demo/openapi/changed-openapi.json`: changed API contract with breaking changes

## Expected Breaking Changes

| Rule | Location | Why it matters |
|---|---|---|
| `PATH_REMOVED` | `/teams` | Existing clients can no longer call the path. |
| `METHOD_REMOVED` | `/users/{id} DELETE` | Existing delete calls break. |
| `REQUIRED_PARAMETER_ADDED` | `/users GET parameter query:role` | Existing calls without `role` can fail validation. |
| `REQUIRED_REQUEST_BODY_ADDED` | `/profile PATCH requestBody` | Existing calls without a body can fail validation. |
| `REQUEST_BODY_REQUIRED_FIELD_ADDED` | `/users POST requestBody application/json.name` | Existing create-user clients must send a new field. |
| `RESPONSE_FIELD_REMOVED` | `/users GET responses.200 application/json[].name` | Clients reading `name` no longer receive it. |
| `RESPONSE_FIELD_TYPE_CHANGED` | `/users GET responses.200 application/json[].id` | Clients expecting numeric `id` now receive a string. |

## Local Reproduction

The `ApiSpecSource` stores a single URL, so the easiest local demo is to serve one mutable file and swap its contents between checks.

```bash
mkdir -p /tmp/apiguard-openapi
cp docs/demo/openapi/baseline-openapi.json /tmp/apiguard-openapi/openapi.json
python3 -m http.server 9000 --directory /tmp/apiguard-openapi
```

1. Create an OpenAPI spec source with `http://localhost:9000/openapi.json`.
2. Run the first spec check. This captures the initial snapshot.
3. In another terminal, replace the served file:

```bash
cp docs/demo/openapi/changed-openapi.json /tmp/apiguard-openapi/openapi.json
```

4. Run the spec check again.
5. Confirm that the diff contains the rules listed above and opens a `CONTRACT_CHANGE` incident for the project.
