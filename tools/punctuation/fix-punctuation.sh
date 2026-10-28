#!/bin/sh
# Dry run of the punctuation clean-up against the local database: sets up the Python venv on
# first run (and again when requirements.txt changes), then runs punctuation.py. Nothing is
# changed in the database; see the report and the SQL files it writes to target/.
set -e

DIR=$(cd "$(dirname "$0")" && pwd)
VENV="$DIR/.venv"

if [ ! -x "$VENV/bin/python" ]; then
	echo "Creating Python venv in $VENV"
	python3 -m venv "$VENV"
fi

if [ ! -f "$VENV/.installed" ] || [ "$DIR/requirements.txt" -nt "$VENV/.installed" ]; then
	echo "Installing Python packages"
	"$VENV/bin/pip" install --quiet --disable-pip-version-check -r "$DIR/requirements.txt"
	touch "$VENV/.installed"
fi

exec "$VENV/bin/python" "$DIR/punctuation.py" "$@"
