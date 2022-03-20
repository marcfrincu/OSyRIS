#!/bin/sh

#NOTE: you cannot change the variable of the parent in a child! Use: source ./add2classpath.sh or add script definition to the .bashrc file

echo "Run this script only once in the console. Each time you run it, it will add the same jars to the CLASSPATH variable"

files=`ls *.jar`;
dir=`pwd`;
for file in ${files}
do
	echo "Adding [ $file ] to CLASSPATH variable";
	path="$dir/$file";
	CLASSPATH="$CLASSPATH:$path";
done 
export CLASSPATH;

echo "Completed";
