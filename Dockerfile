FROM eclipse-temurin:22

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
  org.label-schema.name="oni" \
  org.label-schema.description="A RESTful microservice for managing users and concepts for VARS" \
  org.label-schema.url="https://mbari-media-management.github.io/" \
  org.label-schema.vcs-ref=$VCS_REF \
  org.label-schema.vcs-url="https://github.com/mbari-org/oni" \
  org.label-schema.vendor="Monterey Bay Aquarium Research Institute" \
  org.label-schema.schema-version="1.0" \
  maintainer="Brian Schlining <brian@mbari.org>"

ENV APP_HOME /opt/oni

RUN mkdir -p ${APP_HOME}

COPY oni/target/universal/stage/ ${APP_HOME}/

EXPOSE 8080

ENTRYPOINT $APP_HOME/bin/oni