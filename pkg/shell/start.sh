#!/bin/sh
 
#	License Agreement for Jaeksoft Open Search Server
#	
#	Copyright (C) 2008-2011 Emmanuel Keller / Jaeksoft
#	
#	http://www.open-search-server.com
#	
#	This file is part of Jaeksoft Open Search Server.
#	
#	Jaeksoft Open Search Server is free software: you can redistribute it and/or
#	modify it under the terms of the GNU General Public License as published by
#	the Free Software Foundation, either version 3 of the License, or
#	(at your option) any later version.
#	
#	Jaeksoft Open Search Server is distributed in the hope that it will be useful,
#	but WITHOUT ANY WARRANTY; without even the implied warranty of
#	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#	GNU General Public License for more details.
#	
#	You should have received a copy of the GNU General Public License
#	along with Jaeksoft OpenSearchServer. 
#	If not, see <http://www.gnu.org/licenses/>.

cd `dirname "$0"`
	
EXECUTABLE=apache-tomcat-6.0.32/bin/startup.sh

if [ ! -x "$EXECUTABLE" ]; then
    echo "Cannot find $EXECUTABLE"
    echo "This file is needed to run this program"
    exit 1
fi

CONFIGFILE=/etc/sysconfig/opensearchserver
if [ -f "$CONFIGFILE" ]; then
    . "$CONFIGFILE"
fi

if [ -z "$OPENSEARCHSERVER_DATA" ]; then
     OPENSEARCHSERVER_DATA=`pwd`/data
     export OPENSEARCHSERVER_DATA
fi

exec "$EXECUTABLE"
