Implement a serial communication system inspired by real embedded computers.

The Computer Block should have a `serial` API that allows programs to send text messages and optionally receive messages.

Example:

```cpp
void setup() {
    serial.Begin(9600);

    serial.output("Computer started");
    serial.output("Waiting for materials...");
}
```

Serial output should support multiple destinations:

1. **Minecraft Chat**

Messages can appear in chat with a computer identifier:

```text
[Computer 01] Computer started
[Computer 01] Raw iron detected
[Computer 01] Transfer completed
```

2. **Serial Receiver Block**

Add a placeable Serial Receiver Block that can receive serial messages from a connected computer.

When opened, it should display a custom terminal-style UI containing:

- Connection status.
- Connected computer name.
- Baud rate or communication settings.
- Incoming messages.
- Timestamps.
- Clear-log button.
- Optional scrolling console.
- Connection and error indicators.

Example display:

```text
SERIAL RECEIVER
────────────────────────────
Connected: Computer 01
Baud Rate: 9600

[21:43:02] Computer started
[21:43:03] Raw iron detected
[21:43:03] Raw iron transferred
[21:43:09] Iron ingot received
```

The serial receiver should be a physical Minecraft block that can be placed elsewhere in the world. It should be possible to connect it to a computer through adjacent connections, cables, or a supported network system.

3. **Computer-to-Computer Communication**

Computers should be able to send and receive messages through a communication API.

Example:

```cpp
send(device("farm_controller"), "harvest");
```

A receiving computer could handle the message:

```cpp
serial.on_receive(function(message) {
    if (message == "harvest") {
        farm.harvest();
    }
});
```

The system should support named communication channels or device addresses for larger networks.

Serial communication should be useful for debugging, monitoring factories, displaying machine status, triggering events, and building remote-control systems.