FROM kbase/sdkbase2 as build

WORKDIR /tmp/groups

# dependencies take a while to D/L, so D/L & cache before the build so code changes don't cause
# a new D/L
# can't glob *gradle because of the .gradle dir
COPY build.gradle gradlew settings.gradle /tmp/groups/
COPY gradle/ /tmp/groups/gradle/
RUN ./gradlew dependencies

# Now build the code
COPY deployment/ /tmp/groups/deployment/
COPY jettybase/ /tmp/groups/jettybase/
COPY src /tmp/groups/src/
COPY war /tmp/groups/war/
# for the git commit
COPY .git /tmp/groups/.git/
RUN ./gradlew war

    
FROM kbase/kb_jre:latest

# These ARGs values are passed in via the docker build command
ARG BUILD_DATE
ARG VCS_REF
ARG BRANCH=develop

COPY --from=build /tmp/groups/deployment/ /kb/deployment/
COPY --from=build /tmp/groups/jettybase/ /kb/deployment/jettybase/
COPY --from=build /tmp/groups/build/libs/groups.war /kb/deployment/jettybase/webapps/ROOT.war

# The BUILD_DATE value seem to bust the docker cache when the timestamp changes, move to
# the end
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.vcs-url="https://github.com/kbase/groups.git" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.schema-version="1.0.0-rc1" \
      us.kbase.vcs-branch=$BRANCH \
      maintainer="Steve Chan sychan@lbl.gov"

WORKDIR /kb/deployment/jettybase
ENV KB_DEPLOYMENT_CONFIG=/kb/deployment/conf/deployment.cfg

RUN chmod -R a+rwx /kb/deployment/conf /kb/deployment/jettybase/

# TODO BUILD update to no longer use dockerize and take env vars (e.g. like Collections).
# TODO BUILD Use subsections in the ini file / switch to TOML

ENTRYPOINT [ "/kb/deployment/bin/dockerize" ]

# Here are some default params passed to dockerize. They would typically
# be overidden by docker-compose at startup
CMD [  "-multiline", \
       "-template", "/kb/deployment/conf/.templates/deployment.cfg.templ:/kb/deployment/conf/deployment.cfg", \
       "java", "-Djetty.home=/usr/local/jetty", "-jar", "/usr/local/jetty/start.jar" ]
