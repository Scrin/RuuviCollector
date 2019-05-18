# Contributing

When contributing to this repository, please first discuss the change you wish to make by creating a new Issue about it, if there is no issue about it already.

Issues labeled as "help wanted" are good choices for contributions if you want to contribute but don't have any particular idea in mind. 
If there are no such issues open and you still want to contribute and don't have an idea in mind, you can open an issue about it and we'll figure out something cool for you to do. :)

## Code conventions

There are no ultimately strict code conventions, but try to follow the existing conventions as closely as possible, especially:

- Always have a trailing newline in source files
- Use consistent indentation, most (if not everything?) is 4 spaces
- Avoid unnecessary changes like renaming things or adjusting whitespace, unless there's a good reason for it (ie. correcting a typo/inconsistency)

## Commit conventions

- Use descriptive commit messages. Good examples: "Fix crash in parsing the config" or "Add support for ...". Bad examples: "Fixes", "New feature".
- Write "clean" and easy to read commit messages. Good example: "Add support for custom filtering strategies. Resolves #123" Bad example: "aded support for custom filtering closes issue#123"
- Don't pack too large amount of different changes in single commits, and don't split single features into multiple commits for no reason
- When working on features on your own (feature) branches, if/when you need changes from upstream, prefer [rebase](https://git-scm.com/docs/git-rebase) over merge if possible, to avoid unnecessary merge commits from master.
- Avoid pointless "fix typo" or "pr fixes" commits bloating the history, rather consider using [amend](https://git-scm.com/docs/git-commit#Documentation/git-commit.txt---amend) or [squash](https://stackoverflow.com/questions/5189560/squash-my-last-x-commits-together-using-git) when doing corrections to commits you just did, before they are merged
