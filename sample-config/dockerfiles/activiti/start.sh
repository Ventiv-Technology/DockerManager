#!/bin/bash
set -e
set -x

# mysql db
DB_TYPE=${DB_TYPE:-}
DB_HOST=${DB_HOST:-}
DB_PORT=${DB_PORT:-}
DB_NAME=${DB_NAME:-}
DB_USER=${DB_USER:-}
DB_PASS=${DB_PASS:-}

if [ -z "${DB_HOST}" ]; then
  echo "WARNING: "
  echo "  No mysql connection available."
  echo "  Will work with default H2 in-memory database."
  unset DB_TYPE
fi

# use default port number if it is still not set
case "${DB_TYPE}" in
  mysql)
    DB_PORT=${DB_PORT:-3306}
    sed 's/{{DB_PORT}}/'"${DB_PORT}"'/g' -i /assets/db.properties
    sed 's/{{DB_HOST}}/'"${DB_HOST}"'/g' -i /assets/db.properties
    sed 's/{{DB_NAME}}/'"${DB_NAME}"'/g' -i /assets/db.properties
    sed 's/{{DB_USER}}/'"${DB_USER}"'/g' -i /assets/db.properties
    sed 's/{{DB_PASS}}/'"${DB_PASS}"'/g' -i /assets/db.properties
    cp -f /assets/db.properties /usr/local/tomcat/webapps/activiti-explorer/WEB-INF/classes
    cp -f /assets/db.properties /usr/local/tomcat/webapps/activiti-rest/WEB-INF/classes
    ;;
esac

/usr/local/tomcat/bin/catalina.sh run