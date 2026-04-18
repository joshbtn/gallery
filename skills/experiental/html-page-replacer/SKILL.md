---
name: html-page-replacer
description: Replaces the page with user-provided HTML for rapid SPA-style prototyping and debug logging.
---

# HTML Page Replacer

Use this skill to quickly test HTML page prototypes by replacing the current document with provided HTML.

## Examples

* "Render this HTML page"
* "Load this SPA shell HTML and log debug info"
* "Replace the page with this test markup"

## Instructions

You MUST call the `run_js` tool with the following exact parameters:

- script name: `index.html`
- data: A JSON string with the following fields:
  - html: String. The HTML content to render by replacing the current page.
  - mode: Optional string. `full` (default) replaces the whole document; `body` inserts HTML into a dedicated container `<div>` inside `document.body`.

If user-provided HTML is missing, send:

```json
{
  "html": "<main><h1>Empty HTML input</h1><p>No content was provided.</p></main>",
  "mode": "body"
}
```

After tool execution:

1. Tell the user the page replacement was attempted.
2. Tell the user to check console logs for detailed debug output.
