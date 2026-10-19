Expand the Computer Block into a small in-game operating environment with a virtual file system.

The computer should support multiple saved files and programs rather than only one script.

Example file structure:

```text
Computer/
├── programs/
│   ├── main.cmp
│   ├── furnace.cmp
│   └── farm.cmp
├── config/
│   └── devices.json
└── logs/
    └── latest.log
```

The file system should be virtual and stored with the Minecraft computer block. It should not require access to the player's real computer files.

The custom computer UI should include:

- A file browser.
- Create, open, save, rename, and delete file functionality.
- A code editor with line numbers.
- Syntax highlighting.
- Autocomplete.
- Error highlighting.
- Run, stop, and restart controls.
- Console output.
- Program logs.
- Device inspection.
- Basic API documentation.
- A program status indicator.
- Optional variable inspection and debugging tools.

Programs should be runnable from the UI:

```text
> open programs/main.cmp
> run
```

The system should also allow programs to be started or controlled by other Minecraft blocks.

Examples include:

- A button starting a program.
- A lever stopping a program.
- A redstone signal triggering a program.
- A timer executing a program at intervals.
- A Program Launcher Block running a selected file.
- Another computer remotely starting a program.

Example configuration:

```text
PROGRAM LAUNCHER
────────────────────────
Target: Computer 01
Program: programs/farm.cmp
Trigger: Redstone Signal
```

Add useful computer-management features where appropriate:

- Computer names or IDs.
- Device naming.
- Network discovery.
- Program permissions.
- Manual emergency stop.
- Error logs.
- Reboot or shutdown functionality.
- Optional computer upgrades.
- Configurable communication channels.

The UI should feel like a lightweight IDE or retro computer terminal, with a clear distinction between file management, coding, execution, device management, and serial communication.

The final system should allow players to build programmable networks of computers and Minecraft hardware, creating automation systems limited primarily by their engineering and programming ability.