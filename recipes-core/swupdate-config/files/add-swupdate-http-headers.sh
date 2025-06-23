#!/bin/sh
CONFIG_FILE="/etc/swupdate.cfg"

if [ ! -f $CONFIG_FILE ]; then
    echo "$CONFIG_FILE does not exist" 
    exit 1
fi

SERIAL_NUMBER=$(cat /etc/machine-id)
WFX_URL=$(awk '
/suricatta :/ { in_suricatta = 1; next }
in_suricatta && /url =/ {
    split($0, parts, "\"")
    print parts[2]
    exit
}
in_suricatta && /};/ { exit }
' "$CONFIG_FILE")

# Check if both required header names exist (regardless of values)
SERIAL_EXISTS=$(grep -c "X-Machine-Serial-No" "$CONFIG_FILE")
WFX_URL_EXISTS=$(grep -c "X-Wfx-Url" "$CONFIG_FILE")

if [ "$SERIAL_EXISTS" -gt 0 ] && [ "$WFX_URL_EXISTS" -gt 0 ]; then
    echo "Both required headers (X-Machine-Serial-No and X-Wfx-Url) already exist"
    exit 0
fi

# Add the section if it doesn't exist
if ! grep -q "custom-http-headers:" "$CONFIG_FILE"; then
    echo "Adding custom-http-headers section"
    echo "
custom-http-headers: (
	{ name = \"X-Machine-Serial-No\"; value=\"$SERIAL_NUMBER\" },
	{ name = \"X-Wfx-Url\"; value=\"$WFX_URL\" },
);" >> $CONFIG_FILE

    echo "Successfully added custom-http-headers section"
fi