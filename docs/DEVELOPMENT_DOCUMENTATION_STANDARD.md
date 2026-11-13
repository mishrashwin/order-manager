# Development Documentation Standard

This standard applies to every feature, enhancement, bug-fix, and other development branch in Order Manager Spring Web. A branch is not complete until its applicable documentation is current, reviewed, and included in the branch commit.

## Required branch documents

Create both documents before implementation, using the copy-ready files in [`templates/`](templates/):

1. `docs/branches/<branch-name>.md` — implementation record for future developers.
2. `docs/branches/<branch-name>-TESTING.md` — observable test guide for QA and developers.

Use the exact branch name reported by `git branch --show-current`, replacing path separators or characters invalid in filenames with `-`. Keep the development and testing filenames paired. Register both in [`branches/README.md`](branches/README.md). Read existing branch records before beginning related work and update the existing record when continuing that branch.

## Development document requirements

The branch development document must explain what changed and how it was implemented, without requiring readers to reconstruct the work from Git history. Include the branch name, objective, problem, scope, implemented features, technical summary, database/entity changes, API/controller/service changes, UI/template/static changes, business rules and validation, security and tenant isolation, configuration, migrations, prerequisites, limitations, future enhancements, and commit/PR references when available. Mark inapplicable areas as `None` or `Not applicable` so omissions are explicit.

Update it throughout implementation, including decisions and outcomes. Record specific files or components where useful; avoid copying large code blocks or duplicating the README.

## Testing document requirements

Write for someone who needs to run the application and observe results. Include feature overview/objective, environment and preconditions, test data, roles and permissions, ordered scenarios with expected results, positive and negative validation, authorization and tenant isolation, regression coverage, applicable database/migration and PDF/export/email checks, defect recording guidance, an execution checklist, and a Pass/Fail/Blocked results section. Mark non-applicable test areas explicitly. Record actual execution status and evidence; never claim unrun tests passed.

Automated test requirements in [`testing/TESTING_RULES.md`](testing/TESTING_RULES.md) remain in force. The branch testing guide supplements those tests; it does not replace them. Keep `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` and `docs/testing/TEST_PROGRESS.md` current where existing project rules require it.

## README and project rule updates

Update the root [`README.md`](../README.md) when a feature, enhancement, or significant bug fix changes application capabilities. Update relevant feature/module descriptions and setup, configuration, usage, technology, or architecture information when impacted. Keep README concise and suitable for project discovery; link to detailed branch documentation rather than duplicating implementation notes. Preserve existing material unless its removal is justified.

Update the relevant engineering rule or architecture document when development introduces a lasting pattern, bug fix, or architectural improvement, as directed by [`AGENTS.md`](../AGENTS.md). Keep documentation synchronized in the same branch/PR. Every Java method introduced in future development must include a brief one- or two-line comment describing its purpose, per the repository development instructions.

## Branch workflow and completion checklist

### Before implementation

- [ ] Read this standard, `AGENTS.md`, and relevant architecture, domain, testing, and UI rules.
- [ ] Identify the branch name and scope; inspect `docs/branches/README.md` and any existing branch record.
- [ ] Create the paired development and testing documents from the templates.
- [ ] Record intended README and engineering-rule updates, including why an update is not applicable.

### During implementation

- [ ] Keep the development record current with actual changes and decisions.
- [ ] Add/adjust observable scenarios in the testing guide as behavior changes.
- [ ] Record data, API, UI, business rule, security, configuration, migration, and dependency impacts.
- [ ] Update README and relevant project rules when applicable.

### Before completion

- [ ] README reflects capability/usage changes, or the branch record explains why no README update applies.
- [ ] Development document is complete and describes the final implementation.
- [ ] Testing document covers positive, negative, permission, tenant-isolation, and applicable regression/data/export/email scenarios.
- [ ] Results distinguish Pass, Fail, and Blocked and include only tests actually run.
- [ ] Documentation links resolve and documents agree with final code and behavior.
- [ ] Both branch documents and all relevant documentation updates are included in the branch commit.
- [ ] Final development summary reports the documentation updates.

Missing or materially incomplete required documentation means the branch is not complete.

## Reusable templates

- [Branch development template](templates/BRANCH_DEVELOPMENT_TEMPLATE.md)
- [Branch testing template](templates/BRANCH_TESTING_TEMPLATE.md)
- [Branch document index](branches/README.md)
