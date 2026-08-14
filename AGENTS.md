# RSC Sprite Baker AI Collaboration Rules

This is an independent Git repository coordinated by the Spoiled Milk Core
manager. Sharing a manager does not combine the repositories, branches,
commits, dependencies, releases, or source trees.

Before doing anything, identify the checkout role and run its preflight.

Manager checkout at `/home/justin/rsc-sprite-baker`:

```bash
git status --short --branch
./scripts/ai-manager.sh status
```

Exclusive worker at `/home/justin/rsc-sprite-baker-ai-1`:

```bash
git status --short --branch
./scripts/ai-workspace.sh status
```

## Roles

- `/home/justin/rsc-sprite-baker` owns `main`, review, integration, tests,
  publication, and releases. The Core manager AI may enter this checkout to
  perform those duties; a separate manager session is not required.
- `/home/justin/rsc-sprite-baker-ai-1` is permanently reserved for Sprite
  Baker implementation. It edits only after activation on a focused topic
  branch and hands exact commits back to the manager.
- `/home/justin/Core-Framework` and its `Core-Framework-ai-*` worktrees are not
  implementation locations for this tool.
- `/home/justin/2009scape` is a read-only local compatibility input. Never
  change, clean, commit, or reorganize it from a Sprite Baker task.

## Repository boundary

1. Never merge or cherry-pick Sprite Baker branches into Core `main`.
2. Never activate a Core worker for Sprite Baker work.
3. Do not copy a RuneScape cache, extracted model, texture, animation payload,
   or generated derivative asset into this repository.
4. Tests use independently created neutral fixtures or generate fixtures at
   runtime. Local compatibility tests may read a user-supplied cache path but
   must not redistribute its content.
5. Spoiled Milk integration receives only deliberately reviewed exports or a
   versioned tool dependency through a separate Core task.

## Worker rules

1. One coherent task and one descriptive topic branch at a time. Never edit
   while detached or on `main`.
2. Use `./scripts/ai-workspace.sh checkpoint -m "message"` at meaningful
   milestones and before a session might end.
3. Use `./scripts/ai-workspace.sh handoff -m "message"` only when the exact
   pushed commit is ready for review.
4. Report changed files, tests, compatibility evidence, known risks, and the
   exact READY commit.
5. Compatibility experiments must be deterministic and record cache identity,
   NPC/model/animation identifiers, camera settings, and renderer settings
   without committing source assets.

## Manager rules

1. Keep the manager checkout clean on `main` except during deliberate
   integration and repository management.
2. Begin review with `./scripts/ai-manager.sh status`.
3. Inspect the exact READY branch diff and run relevant tests before merging.
4. Publish tested `main` before recycling the worker.
5. Treat licensing, provenance, and asset redistribution as explicit release
   gates rather than assuming an emulator source license covers cache assets.

## Preservation rules

- Never use `git stash`, `git clean`, `git reset --hard`, forced checkout,
  forced branch deletion, or forced worktree removal as routine workflow.
- Never delete a dirty worker. Preserve and push it first.
- Do not run two AI sessions in the same checkout.
- If repository state is unexpected, stop editing and let the manager inspect
  it.

