# Branch Testing Guide: `<branch-name>`

> Copy to `docs/branches/<branch-name>-TESTING.md`. Replace placeholders, give every scenario observable steps and expected results, and mark inapplicable areas explicitly.

## Feature overview and testing objective
`<What users can do and what this guide verifies.>`

## Preconditions and test environment
- **Application/build:** `<version or branch and start instructions>`
- **Environment/profile:** `<dev/qa/local and relevant setup>`
- **Services/configuration:** `<database, email, storage, flags, or None>`
- **Preconditions:** `<required state before testing>`

## Required test data
- `<Record/data needed, including tenant boundaries where applicable>`

## User roles and permissions
| Role/account | Required permissions | Test purpose |
|---|---|---|
| `<role>` | `<permissions>` | `<purpose>` |

## Test scenarios

### Positive test cases

#### `<ID> — <scenario title>`
1. `<Action>`
2. `<Action>`

**Expected result:** `<Visible result and persisted state where applicable>`

### Negative validation scenarios

#### `<ID> — <invalid/missing/boundary input>`
1. `<Action>`

**Expected result:** `<Validation or error presented; no invalid/partial state saved>`

### Authorization and tenant-isolation tests

#### `<ID> — <role/access or cross-tenant scenario>`
1. `<Action as named role/account>`

**Expected result:** `<Allowed/denied behavior, visible response, and data boundary>`

### Regression tests
- [ ] `<Existing user flow and expected result>`

### Database and migration verification
- [ ] `<Migration applied/version/data integrity check, or Not applicable>`

### PDF, export, and email checks
- [ ] `<Output/delivery/format scenario and expected result, or Not applicable>`

## Defect recording guidance
Record scenario ID, environment/build, role and tenant, exact steps, expected and actual result, evidence (screenshots/logs with sensitive data removed), severity, and reproducibility. Do not include credentials or secrets.

## Test execution checklist
- [ ] Preconditions and test data prepared.
- [ ] Positive scenarios executed.
- [ ] Negative validation scenarios executed.
- [ ] Authorization and tenant-isolation scenarios executed where applicable.
- [ ] Regression scenarios executed.
- [ ] Applicable migration, PDF/export, and email checks executed.
- [ ] Defects recorded and linked.
- [ ] Results below match actual execution.

## Test results

| Scenario / area | Status (Pass / Fail / Blocked) | Tester / date | Evidence or defect reference |
|---|---|---|---|
| `<ID or area>` | `<status>` | `<name / YYYY-MM-DD>` | `<link, notes, or none>` |

**Overall status:** `<Pass / Fail / Blocked / Not run>`

**Blockers or notes:** `<details or None>`
