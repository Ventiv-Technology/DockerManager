FROM tomcat
MAINTAINER John Crygier <john.crygier@ventivtech.com>

ENV ACTIVITI_VERSION 5.17.0
ENV MYSQL_CONNECTOR_JAVA_VERSION 5.1.33
ENV ACTIVITI_URL https://github.com/Activiti/Activiti/releases/download/activiti-$ACTIVITI_VERSION/activiti-$ACTIVITI_VERSION.zip

RUN curl -SL "$ACTIVITI_URL" -o activiti.zip && \
    unzip activiti.zip activiti-$ACTIVITI_VERSION/wars/activiti-explorer.war activiti-$ACTIVITI_VERSION/wars/activiti-rest.war && \
    unzip activiti-$ACTIVITI_VERSION/wars/activiti-explorer.war -d /usr/local/tomcat/webapps/activiti-explorer && \
    unzip activiti-$ACTIVITI_VERSION/wars/activiti-rest.war -d /usr/local/tomcat/webapps/activiti-rest && \
    rm -rf activiti-$ACTIVITI_VERSION && \
    rm -rf activiti.zip

RUN curl -SL "http://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-${MYSQL_CONNECTOR_JAVA_VERSION}.zip" -o mysql.zip && \
    unzip mysql.zip && \
    cp mysql-connector-java-${MYSQL_CONNECTOR_JAVA_VERSION}/mysql-connector-java-${MYSQL_CONNECTOR_JAVA_VERSION}-bin.jar /usr/local/tomcat/webapps/activiti-explorer/WEB-INF/lib/ && \
    cp mysql-connector-java-${MYSQL_CONNECTOR_JAVA_VERSION}/mysql-connector-java-${MYSQL_CONNECTOR_JAVA_VERSION}-bin.jar /usr/local/tomcat/webapps/activiti-rest/WEB-INF/lib/ && \
    rm -rf mysql.zip && \
    rm -rf mysql-connector-java-${MYSQL_CONNECTOR_JAVA_VERSION}


ADD start.sh /usr/local/tomcat/start.sh
RUN chmod a+x /usr/local/tomcat/start.sh
ADD db.properties /assets/db.properties

CMD ["/usr/local/tomcat/start.sh"]