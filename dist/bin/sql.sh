#!/bin/sh
stty erase ^H

if [ "$SQL_HOME" = "" ] ;then
        CURRENT_DIR=`pwd`
        if [ -f $SQL_HOME/bin/sql.sh ]; then
                SQL_HOME=$CURRENT_DIR
        else
                cd ..
                CURRENT_DIR=`pwd`
                SQL_HOME=$CURRENT_DIR
        fi
        export SQL_HOME=$SQL_HOME
fi

java -jar "$SQL_HOME/lib/sql.jar"  com.shell.Console -Dlog=$SQL_HOME/log -Dscript=$SQL_HOME/script -Dtable=$SQL_HOME/table -Dworkspace=$CURRENT_DIR
