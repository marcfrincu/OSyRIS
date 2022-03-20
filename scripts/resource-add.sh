#!/usr/bin/sh

psql -f resources.sql -d osyris

# you might have to run psql -f osyris.sql -d osyris -U username
