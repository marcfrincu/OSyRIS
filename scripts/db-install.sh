#!/usr/bin/sh

psql -f osyris.sql

psql -f stored_procedures.sp

# you might have to run psql -f osyris.sql -U username
