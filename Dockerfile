FROM kbase/kb_jre:latest as build
RUN apt-get -y update && apt-get -y install ant git openjdk-8-jdk
RUN cd / && git clone https://github.com/kbase/jars

ADD . /src
RUN cd /src && ant build
RUN find /src

FROM kbase/kb_jre:latest

COPY --from=build /src/deployment/ /kb/deployment/
COPY --from=build /src/jettybase/ /kb/deployment/jettybase/

# Variables should be populated by Dockerhub during the automated
# build.
LABEL \
      org.label-schema.vcs-url="https://github.com/kbase/groups.git" \
      org.label-schema.vcs-ref=$SOURCE_COMMIT \
      org.label-schema.schema-version="1.0.0-rc1" \
      us.kbase.vcs-branch=$SOURCE_BRANCH \
      maintainer="Steve Chan sychan@lbl.gov"

WORKDIR /kb/deployment/jettybase
ENV KB_DEPLOYMENT_CONFIG=/kb/deployment/conf/deployment.cfg

RUN chmod -R a+rwx /kb/deployment/conf /kb/deployment/jettybase/

ENTRYPOINT [ "/kb/deployment/bin/dockerize" ]

# Here are some default params passed to dockerize. They would typically
# be overidden by docker-compose at startup
CMD [  "-multiline", \
       "-template", "/kb/deployment/conf/.templates/deployment.cfg.templ:/kb/deployment/conf/deployment.cfg", \
       "java", "-Djetty.home=/usr/local/jetty", "-jar", "/usr/local/jetty/start.jar" ]
