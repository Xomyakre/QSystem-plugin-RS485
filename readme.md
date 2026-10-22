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

### 1. Download the Plugin

Download the following files required for installation:

**Main plugin**
- `Clientboardrs485plugin-10.2.jar` — the compiled plugin containing the project implementation.

**Required libraries and scripts**
- `rxtxParallel.dll`
- `rxtxSerial.dll`
- `adminRS485.bat` — for Windows
- `adminRS485.sh` — for Linux

**Configuration**
- `config/ClientboardRS485Plugin.xml`

### 2. Install the Plugin

Copy the downloaded files into the appropriate QSystem directories, replacing the existing files where required.

Make sure that the plugin JAR, required libraries, startup script, and configuration file are placed in their correct locations.


## Compatibility

### Java 1.8 and later

