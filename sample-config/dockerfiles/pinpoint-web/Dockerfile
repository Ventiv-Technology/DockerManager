# docker run -d -p 8080:8080 -e HBASE_HOST=192.168.59.103 -e HBASE_PORT=49178 pinpoint-web
# Current Issues: Pinpoint Web wants to run as the root application.  E.g. There are requests to http://host/applications.pinpoint instead of http://host/pinpoint-web/applications.pinpoint

FROM tomcat:8-jre8
MAINTAINER John Crygier <john.crygier@ventivtech.com>

ENV PINPOINT_VERSION 1.0.4

RUN curl -SL "https://github.com/naver/pinpoint/releases/download/$PINPOINT_VERSION/pinpoint-web-$PINPOINT_VERSION.war" -o pinpoint-web.war && \
    unzip pinpoint-web.war -d /usr/local/tomcat/webapps/pinpoint-web && \
    rm -rf pinpoint-web.war

ADD start.sh /usr/local/tomcat/start.sh
RUN chmod a+x /usr/local/tomcat/start.sh

ADD hbase.properties /assets/hbase.properties
ADD pinpoint-web.properties /assets/pinpoint-web.properties

CMD ["/usr/local/tomcat/start.sh"]