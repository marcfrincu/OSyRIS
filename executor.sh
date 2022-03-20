#!/bin/sh

# This script is responsible for starting an agent module
# It is usually called by the Healing module
# Arguments:

# $1 silkFile 
# $2 solution ID
# $3 property file
# $4 parent workflow ID
# $5 module type: HEALING|WORKFLOW

if [ $# -eq 6 ]; then
	cd "$6/bin/"
else
	cd "$5/bin/"
fi

THE_CLASSPATH=

for i in `ls ../lib/*.jar`
do
	THE_CLASSPATH=${THE_CLASSPATH}:${i}
done;

for i in `ls ../lib/drools-5.1.0/*.jar`
do
	THE_CLASSPATH=${THE_CLASSPATH}:${i}
done;

#echo $THE_CLASSPATH

if [ $5 = "WORKFLOW" ]; then
	# $1 silkFile 
	# $2 solution ID
	# $3 property file
	# $4 parent workflow ID
	# $5 module type: HEALING|WORKFLOW
	# $6 remote dir

	java -cp ".$THE_CLASSPATH" osyris.distributed.DOSyRIS $1 $2 "../$3" $4
else
	# $1 deployment file
	# $2 ssh username
	# $3 property file
	# $4 module type: HEALING|WORKFLOW
	# $5 remote dir
		
	java -cp ".$THE_CLASSPATH" osyris.distributed.healing.Healing $1 $2 "../$3"
fi