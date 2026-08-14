# AI Workspace ai-1

This folder is a neutral AI worktree slot. Its current state is:

- Phase: `ACTIVE`
- Branch: `feat/cache-compatibility-spike`
- Checkpoint: `c011240d2c04898864351f1a59ac51c40fc7e7d0`

Project boundary: this slot belongs only to Spoiled Milk/Core-Framework. Never accept World Editor, World Builder, runtime-provider, or provider-lock work here. Those tasks belong to `/home/justin/rsc-world-editor` or `/home/justin/rsc-world-editor-runtime` and their independent workers.

Work only on the assigned branch and task. Do not switch branches, use Git stashes, run the public hosted server, or alter another worktree.

After meaningful progress, preserve it with:

```bash
./scripts/ai-workspace.sh checkpoint -m "Describe the checkpoint"
```

Before ending the session, make the exact pushed commit available to the manager with:

```bash
./scripts/ai-workspace.sh handoff -m "Describe the handoff"
```
