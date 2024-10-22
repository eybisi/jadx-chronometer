#!/bin/bash

# Check if Gum is installed
if ! command -v gum &> /dev/null; then
    echo "Gum is not installed. Please install it first: https://github.com/charmbracelet/gum"
    exit 1
fi

# Step 1: Uninstall the existing plugin
gum style --foreground 212 "Uninstalling existing jadx-chronometer plugin..."
if jadx plugins --uninstall jadx-chronometer &> /dev/null; then
    gum style --foreground 46 "Uninstalled successfully."
else
    gum style --foreground 196 "Uninstall failed."
    exit 1
fi

# Step 2: Build the plugin
gum style --foreground 212 "Building the plugin..."
if ./gradlew build &> /dev/null; then
    gum style --foreground 46 "Build successful."
else
    gum style --foreground 196 "Build failed."
    exit 1
fi

# Step 3: Install the newly built plugin
gum style --foreground 212 "Installing the newly built plugin..."
if jadx plugins --install file:$HOME/Github/jadx-chronometer/build/libs/jadx-chronometer-dev.jar &> /dev/null; then
    gum style --foreground 46 "Installed successfully."
else
    gum style --foreground 196 "Installation failed."
    exit 1
fi

# Final message
gum style --foreground 49 "jadx-chronometer plugin update complete!"