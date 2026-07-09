# Installing the Terracotta CLI

This page describes how to download and install the Terracotta CLI.

## System Requirements

- **Linux**: x86_64 architecture, glibc 2.17+
- **macOS**: x86_64 or Apple Silicon (universal binary)
- **Windows**: x86_64, Windows 10 or later

## Installation

### GitHub Releases

Download compiled native binaries from the [Releases](https://github.com/beduality/terracotta/releases) page.

=== "Linux"

    1. Download the `terracotta-linux-amd64` binary
    2. Make it executable:
       ```bash
       chmod +x terracotta-linux-amd64
       ```
    3. Move it to your PATH:
       ```bash
       sudo mv terracotta-linux-amd64 /usr/local/bin/terracotta
       ```
    4. Verify installation:
       ```bash
       terracotta --version
       ```

=== "macOS"

    1. Download the `terracotta-macos-universal` binary
    2. Make it executable:
       ```bash
       chmod +x terracotta-macos-universal
       ```
    3. Move it to your PATH:
       ```bash
       sudo mv terracotta-macos-universal /usr/local/bin/terracotta
       ```
    4. Verify installation:
       ```bash
       terracotta --version
       ```

=== "Windows"

    1. Download the `terracotta-windows-amd64.exe` binary
    2. Create a directory for Terracotta (e.g., `C:\Tools\Terracotta`)
    3. Move the executable to that directory
    4. Add the directory to your system PATH:
       - Press `Win + X` and select "System"
       - Click "Advanced system settings"
       - Click "Environment Variables"
       - Under "System variables", select "Path" and click "Edit"
       - Click "New" and add your Terracotta directory
       - Click "OK" to save changes
    5. Restart your terminal and verify installation:
       ```cmd
       terracotta --version
       ```

## Verifying Installation

After installation, verify that Terracotta is working correctly:

```bash
terracotta --help
```

This should display the CLI help message with available commands.

## Upgrading

To upgrade to a new version:

1. Download the latest binary from the [Releases](https://github.com/beduality/terracotta/releases) page
2. Replace the existing binary with the new one
3. Verify the upgrade with `terracotta --version`
