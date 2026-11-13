# Branch Testing Guide: `documentation`

## Feature overview and testing objective
This branch establishes documentation requirements and reusable templates. Validation confirms that the standard, templates, instructions, and index exist, cross-link correctly, include required sections, and do not change application code or business functionality.

## Preconditions and test environment
- **Application/build:** Repository checkout on the documentation-standard branch; no application launch required.
- **Environment/profile:** Local repository workspace with Git and PowerShell.
- **Services/configuration:** None.
- **Preconditions:** Documentation changes applied; working tree available for inspection.

## Required test data
No application data is required. Confirm the paired documents are named `documentation.md` and `documentation-TESTING.md`.

## User roles and permissions
| Role/account | Required permissions | Test purpose |
|---|---|---|
| Developer / repository contributor | Read repository and inspect Git changes | Verify standard and file scope |
| QA / documentation reviewer | Read Markdown files and follow relative links | Verify usable testing guidance |

## Test scenarios

### Positive test cases

#### DOC-01 — Required standard files are present
1. Inspect the root, `docs`, `docs/templates`, and `docs/branches` directories.
2. Confirm the standard, both templates, index, and this branch's development/testing pair are present.

**Expected result:** All required files exist at their specified paths.

#### DOC-02 — Templates cover the required content
1. Review the development template against Standard section “Development document requirements”.
2. Review the testing template against Standard section “Testing document requirements”.

**Expected result:** Placeholders and structured checklists cover every mandatory field and scenario category; non-applicable items can be marked explicitly.

#### DOC-03 — Markdown links resolve
1. Check relative links in README, AGENTS, the standard, index, and paired branch documents.
2. Confirm links target files or directories that exist.

**Expected result:** All repository-relative Markdown links resolve.

#### DOC-04 — README and Codex instructions enforce the standard
1. Review README's documentation section.
2. Review AGENTS.md's before, during, and completion requirements.

**Expected result:** README briefly points to the standard; AGENTS.md requires paired records, applicable README updates, synchronized docs, and completion reporting.

### Negative validation scenarios

#### DOC-05 — Application functionality is unchanged
1. Inspect the changed-file list with `git status --short` and `git diff --name-only`.
2. Confirm changes are limited to Markdown documentation.

**Expected result:** No Java, entity, repository, controller, service, migration, or application configuration files are modified.

### Authorization and tenant-isolation tests
Not applicable: this documentation-only change adds no runtime routes, data access, or authorization behavior. Confirm the testing template includes these checks for future branches where applicable.

### Regression tests
- [ ] Confirm existing architecture, testing, UI, deployment, and business documentation files remain present and were not overwritten.
- [ ] Confirm existing branch documents remain linked in the index.

### Database and migration verification
- [ ] Not applicable: no schema, database, entity, or migration changes.

### PDF, export, and email checks
- [ ] Not applicable: no PDF, export, or email behavior changes.

## Defect recording guidance
Record the document path, missing or incorrect section/link, expected requirement, actual content, and screenshot or command output if useful. Do not include credentials or secrets.

## Test execution checklist
- [ ] Required files and paths verified.
- [ ] Development and testing template sections verified.
- [ ] Relative links checked.
- [ ] README and AGENTS.md requirements checked.
- [ ] Changed-file scope checked for documentation-only changes.
- [ ] Existing documentation presence checked.
- [ ] Results below reflect actual validation.

## Test results

| Scenario / area | Status (Pass / Fail / Blocked) | Tester / date | Evidence or defect reference |
|---|---|---|---|
| DOC-01 — required files and paths | Pass | Codex / 2026-11-13 | Required paths present |
| DOC-02 — template sections | Pass | Codex / 2026-11-13 | All mandatory headings detected in templates |
| DOC-03 — relative links | Pass | Codex / 2026-11-13 | Markdown link scan; corrected README architecture link |
| DOC-04 — README and AGENTS requirements | Pass | Codex / 2026-11-13 | README references standard; existing AGENTS.md mandatory checklist contains required phases |
| DOC-05 — documentation-only changed paths | Pass | Codex / 2026-11-13 | New/modified files for this task are Markdown |
| Existing documentation preservation | Pass | Codex / 2026-11-13 | Original engineering rule files and branch docs remain present |

**Overall status:** Pass

**Blockers or notes:** No application test suite was run because this change is documentation-only. The working tree also contains unrelated pre-existing changes (including moved documentation paths and an untracked archive); they were left untouched and are not part of the documented change set.
