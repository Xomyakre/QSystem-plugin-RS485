# QSystem RS485 Plugin

A plugin for [QSystem](https://github.com/malashta/apertum-qsystem.rs485-plugin) that extends the original functionality by adding support for an additional RS485 communication protocol.

## Overview

This project extends the existing QSystem plugin to support a new protocol for communicating with compatible hardware.

The goal is to provide compatibility with devices that use a communication protocol not supported by the original implementation.

## Installation

Follow the steps below to install and configure the plugin.

### Prerequisites

- A working QSystem installation
- [Java Runtime Environment](https://www.java.com/) compatible with your QSystem version
- The required hardware and connection interface

### Steps

1. **Download the plugin**

   Download the file Clientboardrs485plugin-10.2.jar. It contains the entire project compiled. You will also need the required .dll files ----> rxtxParallel.dll //// rxtxSerial.dll and the adminRS485.bat/adminRS485.sh file (depending on the system)

   Optional files to download ----> config/ClientboardRS485Plugin.xml

2. **Copy the plugin files**

   Replace file in QSystem


## Compatibility

Compatibility depends on the QSystem version, Java runtime, hardware, and supported communication protocol.

## License

Refer to the original project's license and applicable third-party dependencies.
