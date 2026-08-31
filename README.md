# Weldr

Build MCP (Model Context Protocol) servers using only spreadsheet formulas — no coding required — and have AI assistants 
call them in your workflows.

## What this is

This tool lets business users turn spreadsheet logic into fully functional MCP servers that AI assistants like Claude can call directly. If you can write a formula, you can build an MCP server tool.

Each MCP server is backed by an XLSX workbook. You define your inputs, write your business logic as formulas, and define your outputs — the same way you'd build any spreadsheet. Weldr wraps that workbook in an MCP server automatically, so it can be called by Claude (or any MCP-compatible client) as a live tool.

## Why spreadsheets

- **No new syntax to learn.** If you can write `=VLOOKUP(...)` or `=IF(...)`, you already know how to write the business logic.
- **Logic stays visible.** Anyone who understands spreadsheets can open the workbook and see exactly how a result was produced — no hidden code path.
- **Fast to build, fast to fix.** Iterating on a formula is faster than iterating on a codebase for most non-developers.

## How it works

At a high level, the following is true:
1. **Define your input sheets.** Each input becomes a parameter the MCP server accepts (typically an absolute file path to a CSV).
2. **Write your formulas.** Reference the input sheets like any other spreadsheet data, and build your logic using standard formulas.
3. **Define your output sheet(s).** Whatever your formulas produce becomes the data returned by the tool.
4. **Package and deploy.** The tool handles wrapping your workbook as an MCP server — no server code required.

Read below for more detailed requirements on the input and output sheet requirements:

Each workbook should have at least one output sheet and zero or more input sheets.  It should also have a `metadata` sheet.

### Input Sheets
1. Input sheets have sheet names prefixed with `input_`, such as `input_customer_addresses` or `input_transaction_categories`.  
2. Input sheets should have a header row with column names starting in cell A1.
3. Optionally (but recommended), the columns of the input sheet should have data types (ex: General, Integer, Date Time, Boolean, etc).  These data types will be enforced when your MCP server is called.
4. Optionally (but recommended), you can add comments to the header cells, which will be visible to the AI assistants to help them understand context about the header's data.

Each input sheet becomes a parameter the MCP server accepts (typically an absolute file path to a CSV).

### Output Sheets
Overall, output sheets should be the same as input sheets, but with a few differences:

1. Output sheets have sheet names prefixed with `output_`, such as `output_customer_information` or `output_invalid_categories`.
2. The first column should named `include_in_output` and should be of the data type boolean.  If the value in this column is `true`, then the row will be included in the workbook output.  If `false` it will be excluded.  This column will never be included in the workbook output.
3. Any column name prefixed with `ignore_` will be excluded from the workbook output.  These will likely be helper columns in your calculations.

### The Metadata Sheet
1. Every workbook must have a sheet named `metadata`.  
2. Cell A1 must have a formula that returns the number of errors in the workbook's calculations.  The simplest way to do 
this is to write a formula that sums up all the errors in your output sheet(s).  For example, if you have one output sheet 
named `output_invalid_categories`, then the formula could be `=SUMPRODUCT(--ISERROR($output_invalid_categories.A1:G501))` 
(change the cell range to your situation).  If the formula returns 0, then the Java wrapper interprets that to mean the calculations
were successful and it begins extracting data from the output sheets.  If the formula returns a non-zero number, then the 
Java wrapper interprets that to mean the calculations were NOT successful and it sends an error to the AI assistant.

Outside of these requirements, you can write your business logic with whatever formulas and columns you want.

## Available tool calls

Every generated MCP server exposes a consistent set of calls:

| Tool | Purpose |
|---|---|
| `get_manifest_of_<workbook>` | Returns the input/output schema (sheet names, fields, types) without running anything. |
| `run_workbook_engine_for_<workbook>` | Executes the workbook against the given input file path(s). |

### `run_workbook_engine` parameters

| Parameter | Description |
|---|---|
| Input sheet paths (e.g. `input_categories`, `input_transactions`) | Absolute file path(s) to the input CSV file(s) for each defined input sheet. |
| `return_outputs` | If `true`, the tool response includes the computed output data directly. If `false`, only the output file path is returned. |
| `debug_mode_enabled` | If `true`, the full XLSX workbook is written to a storage location and its file path is returned, so you can open it directly to inspect formulas, intermediate values, and results. |

## Debugging workflow

This is the most important habit to build:

1. Run your workbook with `debug_mode_enabled: true`.
2. Open the returned `.xlsx` file path directly.
3. Trace the formula logic against the actual input data, cell by cell, the same way you'd debug any spreadsheet.

Because the workbook itself — not a log file or stack trace — is the debugging artifact, anyone who can read a spreadsheet can debug a broken tool.

## Versioning

Spreadsheet software (e.g. LibreOffice) auto-increments a version number in the workbook's document properties on save. The tool reads this version automatically, giving every workbook a built-in revision history without any extra setup.

This solves *"has this workbook changed since I last trusted it, and can I roll back?"* It does not, by itself, solve *"what exactly changed, and is the change correct?"* — that still requires opening the file. Where possible, use your spreadsheet tool's built-in diffing features - such as [LibreOffice's Compare Document feature](https://ask.libreoffice.org/t/how-do-i-compare-two-spreadsheet-files/20147/5) - to inspect formula-level changes between versions, not just changed cell values, before trusting a new version in production.

## Common pitfalls

These are easy to hit and easy to miss, since spreadsheet logic tends to fail *quietly* rather than throwing an error:

- **Header row mismatches.** All CSVs should have a header row.
- **Case and whitespace sensitivity.** Formula-based matching (`VLOOKUP`, `MATCH`, `IF` comparisons) is often stricter about exact string matches than you'd expect. Trailing spaces or inconsistent casing between your input data and your lookup values can cause silent mismatches.
- **Silent failures without the error-count convention.** If a workbook doesn't populate its error-count cell for a given failure mode, that failure will look like a quiet success. Treat the error-count cell as required for every calculation path, not just the obvious ones.
- **Leaving input columns as the `General` data type. Under the hood, Weldr uses Java and Apache POI.  Declaring a specific type (Integer, Date Time, Boolean, etc.) lets the Java reject bad values before they reach your formulas. Leaving a column as `General` skips that check, so malformed input can flow straight into your calculations instead of being caught upfront.  It's recommended you use the `General` data type only for strings/text, not numbers, dates, etc.

## Planned: test scenarios (post-MVP)

A planned feature will let users supply multiple test scenarios — input CSVs paired with expected output CSVs — that the tool checks automatically before packaging the JAR and MCPB files. This acts like a unit test suite, without requiring any code:

- Define a scenario as an input file (or set of input files) plus the output you expect.
- The tool runs the workbook against each scenario and compares actual output to expected output.
- A failing scenario blocks the build, the same way a failing test blocks a code deployment.

The main value of this isn't just catching bugs before first release — it's making it cheap to **re-run the same scenarios after every future logic change**, so edits to a workbook get regression-checked automatically rather than only validated when something visibly breaks downstream.

## Scope: ETL-style transformations, not live orchestration

Spreadsheet-backed MCP servers are best suited to acting as **mini ETL jobs**: all data is retrieved up front (e.g. as CSV exports) and fed into the workbook as a transformation step. They are not suited to live API calls, orchestration, or anything requiring a network round-trip mid-calculation.

Within that ETL scope, spreadsheets can go further than a simple lookup:

- **Nested conditionals** — build with chained helper columns rather than one deeply nested formula.
- **Cross-sheet joins** — handled with `VLOOKUP` (or `INDEX`/`MATCH`) between sheets.

Keeping each helper column's job small and clearly named is what keeps this style of workbook traceable as logic grows — see Best practices below.

## Best practices

- Require an error-count cell (and ideally an error-detail sheet) in every workbook — treat a missing one as incomplete, not optional.
- Build a small, known test dataset alongside your real data, and re-run it after every logic change (or use the test scenarios feature once available).
- Keep one formula's job simple — prefer several clearly-named helper columns over one giant nested formula.
- Name your input/output sheets descriptively; they become the parameter names AI assistants and other callers will see.
- Always sanity-check `debug_mode_enabled` output the first time you deploy a new or changed workbook, not just when something looks broken.
- Before trusting a new workbook version, check the spreadsheet tool's diff view for formula-level changes, not just the auto-incremented version number.