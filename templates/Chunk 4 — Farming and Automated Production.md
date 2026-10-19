Expand the programmable computer system into farming and production automation.

The player should be able to use code to control farming equipment, inspect crop states, harvest resources, replant crops, and manage storage.

Supported farming devices could include:

- Crop sensors.
- Planters.
- Harvesting machines.
- Robotic farming tools.
- Irrigation systems.
- Greenhouses.
- Tree farms.
- Animal farming equipment.
- Storage and sorting systems.

The exact devices can depend on the Minecraft implementation, but they should expose clear APIs to the programming language.

Example:

```cpp
void setup() {
    serial.Begin();

    farm = device("wheat_farm");
    seeds = device("seed_storage");
    storage = device("crop_storage");
}

void loop() {
    if (farm.crop.is_mature()) {
        farm.harvest();

        serial.output(
            timestamp() + ": Wheat harvested"
        );
    }

    if (farm.crop.is_empty()) {
        if (seeds.contains(item.minecraft.wheat_seeds)) {
            transfer(
                item.minecraft.wheat_seeds,
                seeds,
                farm
            );

            farm.plant();

            serial.output(
                timestamp() + ": Wheat replanted"
            );
        }
    }

    if (farm.inventory.contains(item.minecraft.wheat)) {
        transfer(
            item.minecraft.wheat,
            farm.inventory,
            storage
        );
    }
}
```

The system should also support general production automation, such as:

- Supplying furnaces with input materials.
- Supplying fuel.
- Moving finished products.
- Monitoring machine progress.
- Sorting items into storage.
- Detecting full inventories.
- Managing multi-stage production chains.
- Monitoring power, fuel, or fluid levels.
- Shutting down systems when resources are unavailable.

Programs should be able to respond to machine states and failures instead of assuming every operation succeeds.

For example:

```cpp
if (!transfer(item.minecraft.raw_iron, chest, furnace)) {
    serial.error("Unable to supply furnace");
}
```

The goal is to make farming and factory automation depend on the player's programming and system design.