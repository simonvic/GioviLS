# Giovi Language Server

> [!WARNING]
> This is a highly experimental throw-away prototype, not suited yet for actual usage.

An implementation of the [Language Server Protocol](https://microsoft.github.io/language-server-protocol).

## Getting started

1. Clone the repo
  ```bash
  $ git clone https://github.com/simonvic/GioviLS.git
  ```

2. Initialize submodules: this project relies on the grammar files provided by
  [GioviSpecs](https://github.com/simonvic/GioviSpecs); before building the
  project, make sure to initialize the submodules
  ```bash
  $ git submodule update --init
  ```

3. Build with [maven](https://maven.apache.org/).
  ```bash
  $ mvn package
  ```

## Integration

This implementation aims to be editor-agnostic; it should work with any
compliant client (e.g. text editor).

Refer to your editor documentation on how to configure the language server.

### Neovim

For Neovim, you can configure it by adding the following snippet to
`after/lsp/giovils.lua`

```lua
---@type vim.lsp.Config
return {
	name = "giovils",
	cmd = {
		"java",
		"-jar",
		"/path/to/GioviLS/target/GioviLS.jar"
	},
	filetypes = { "giovi" },
	settings = {},
}
```

And then, add `vim.lsp.enable({"giovils"})` in your `init.lua` to make it start
automatically for every giovi file.

If neovim doesn't recognize the `giovi` filetype, add the following to your
`init.lua`

```lua
vim.filetype.add({ extension = { giovi = "giovi" } })
```

## Developement

To aid developement, `giovils` allows to be started with the `--proxy` argument
allowing you to debug and hotswap/hotreload the server with your IDE.

1. With your IDE, launch the server via `GioviLSSocket.java`; it will listen
   for connections on `localhost:8069`
2. With your client, launch the server with the `--proxy` argument; it will
   connect to `localhost:8069` and relay all protocol comunications to the
   actual server.

