# AI logs

This project was built with **Claude Code** (desktop app, model `claude-opus-5-5`) in a single session on 23 Oct 2026. These files come from that session's export.

| File | What it is |
|---|---|
| [claude-code-session.md](claude-code-session.md) | Readable transcript. Every prompt and reply in order, my answers to Claude's questions, and one line per tool call (commands run, files written, diagrams shown). Start here. |
| [claude-code-session.jsonl](claude-code-session.jsonl) | The same conversation as raw JSON lines, one message per line, with the full tool call inputs and outputs. |

The JSONL contains only user and assistant messages. The rest of the export was left out: the desktop app's session settings, account and organisation identifiers, environment snapshots, and internal bookkeeping entries. My work email address is replaced with `[work email removed]` in both files; nothing else in the conversation was edited.

The log covers the session up to the request to re-export it, so the last few messages (the re-export and push itself) aren't included.

The important interactions, and what I kept, changed or rejected, are summarised in [BUILD_LOG.md](../BUILD_LOG.md), section 4.
