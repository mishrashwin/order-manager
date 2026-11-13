# Branch Documentation Index

Branch records preserve the objective, implementation decisions, and validation guidance for completed and in-progress development work. Use this index to find prior work without reconstructing it from Git history.

## Naming convention

For branch `<branch-name>`, maintain the paired files:

- `docs/branches/<branch-name>.md` — development record.
- `docs/branches/<branch-name>-TESTING.md` — QA and developer testing guide.

Use the repository's actual branch name, as returned by `git branch --show-current`. Start from [`../templates/BRANCH_DEVELOPMENT_TEMPLATE.md`](../templates/BRANCH_DEVELOPMENT_TEMPLATE.md) and [`../templates/BRANCH_TESTING_TEMPLATE.md`](../templates/BRANCH_TESTING_TEMPLATE.md). See the [Development Documentation Standard](../DEVELOPMENT_DOCUMENTATION_STANDARD.md) and [`../../AGENTS.md`](../../AGENTS.md) for required workflow.

## Branch records

| Branch / work | Development document | Testing guide |
|---|---|---|
| Company GSTN fix summary | [COMPANY_GSTN_FIX_SUMMARY.md](COMPANY_GSTN_FIX_SUMMARY.md) | Not present |
| Vendor upload via Excel | [Vendor_Upload_via_Excel.md](Vendor_Upload_via_Excel.md) | Not present |
| Vendor PO | [Vendor-PO.md](Vendor-PO.md) | Not present |
| Vendor PO terms and conditions | [Vendor-PO-TnC.md](Vendor-PO-TnC.md) | Not present |
| Development documentation standard (`documentation`) | [documentation.md](documentation.md) | [documentation-TESTING.md](documentation-TESTING.md) |

“Not present” identifies an absent historical testing guide; no historical records are fabricated. Add rows when branch documents are created, and keep links paired for new work.
