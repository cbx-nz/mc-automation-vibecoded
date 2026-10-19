Design a beginner-friendly custom programming language for the Minecraft Computer Block. The syntax should be inspired by C, C++, and embedded programming, but it does not need to be actual C++.

The language should support:

- `setup()` functions that run once.
- `loop()` functions that run repeatedly.
- Variables.
- Strings, numbers, and booleans.
- Conditions.
- Loops.
- Functions.
- Timers.
- Events.
- Device references.
- Item identifiers.
- Error handling.

Example syntax:

```cpp
void setup() {
    serial.Begin();

    chest = input(chest, 0, 0, 0);
    furnace = output(furnace, 0, 1, 0);
}

void loop() {
    if (chest.contains(item.minecraft.raw_iron)) {
        transfer(
            item.minecraft.raw_iron,
            chest,
            furnace
        );

        serial.output(
            timestamp() + ": Raw iron transferred"
        );
    }
}
```

Support both coordinate-based and named device references:

```cpp
chest = input(chest, 0, 0, 0);
```

```cpp
chest = device("iron_input");
furnace = device("iron_furnace");
```

Create an inventory API with functions such as:

```cpp
chest.contains(item.minecraft.raw_iron);
chest.count(item.minecraft.raw_iron);
chest.is_full();
chest.is_empty();
```

And transfer functions such as:

```cpp
transfer(item.minecraft.raw_iron, chest, furnace);
transfer(item.minecraft.raw_iron, chest, furnace, amount = 16);
transfer_all(chest, storage);
```

Transfers must not silently delete items. They should return a success/failure result and provide a reason when unsuccessful, such as a full inventory or incompatible destination.

Machines should expose APIs appropriate to their type. For example, furnaces may expose input, fuel, output, processing state, progress, and completion status.

Support event-based functionality where appropriate, such as inventory changes, machine completion, redstone signals, and timers, while retaining `loop()` for beginners.

Programs should have execution limits to prevent infinite loops or expensive code from freezing the game or server.