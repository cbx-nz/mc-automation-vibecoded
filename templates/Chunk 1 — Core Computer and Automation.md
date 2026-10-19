Create a Minecraft-inspired programmable computer and automation system. The central feature is a placeable Computer Block that players can interact with to open a custom computer interface, write programs, execute code, and control supported Minecraft blocks.

The system should feel like a combination of a small embedded computer, an in-game IDE, Minecraft machinery, and The Farmer Was Replaced. The player should build the hardware and write the software that operates it.

The Computer Block should have a custom UI containing:

- A terminal or desktop-like interface.
- A virtual file browser.
- A code editor.
- A console output panel.
- Run, stop, and restart controls.
- Program status indicators.
- Connected-device information.
- Basic computer settings.

Computers should be able to interact with nearby or connected blocks, such as:

- Chests and other inventories.
- Furnaces and processing machines.
- Hoppers and item-transfer devices.
- Redstone components.
- Sensors.
- Farming machines.
- Other Computer Blocks.
- Serial Receiver Blocks.

The system should be designed around programmable infrastructure rather than fixed-purpose automation. A player should be able to create their own item sorter, furnace controller, farm controller, alarm system, or factory management system using code.

Programs should run in a controlled, sandboxed environment. Do not execute arbitrary native C++, JavaScript, Python, or operating-system commands. The custom language should interact with Minecraft through a restricted API.

The system should support persistent computers, saved programs, multiplayer-safe execution, controlled resource access, error handling, and execution limits.

The overall goal is to let players build a computer, connect hardware, write software, and make the Minecraft world respond to their code.