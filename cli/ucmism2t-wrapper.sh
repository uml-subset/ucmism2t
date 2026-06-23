#!/bin/bash
# ucmism2t Wrapper - Suppresses Eclipse launcher stderr output

if [ ! -f "$LAUNCHER" ]; then
    echo "Error: ucmism2t launcher not found" >&2
    exit 1
fi

# Run launcher and filter out its stderr noise
"$LAUNCHER" "$@" 2> >(grep -v -E "^(JVM terminated|/.*java$|-X(ms|mx)|-Dfile|-jar|-os |-ws |-arch |-launcher |-name |--launcher|-startup |-exitdata |-vm |-vmargs|Ucmism2t:$)" >&2)
exit $?
