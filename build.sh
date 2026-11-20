#!/usr/bin/env bash

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
echo $SCRIPT_DIR

ARCH=$(uname -m)
if [[ $ARCH == 'arm64' ]]; then
    # Stage the Docker build context, then find where sbt put it.
    # sbt 2.0 changed the staging directory from target/docker/stage to
    # target/out/jvm/scala-X.Y.Z/<project>/docker/stage.
    sbt 'Docker / stage'
    STAGE_DIR=$(find "$SCRIPT_DIR/target" -name "Dockerfile" -not -path "*/streams/*" | head -1 | xargs dirname)

    if [[ -z "$STAGE_DIR" || ! -f "$STAGE_DIR/Dockerfile" ]]; then
        echo "ERROR: could not locate Docker staging directory after 'Docker / stage'" >&2
        exit 1
    fi
    
    cd "$STAGE_DIR"
    VCS_REF=`git tag | sort -V | grep -E '^\d' | tail -1`
    # https://betterprogramming.pub/how-to-actually-deploy-docker-images-built-on-a-m1-macs-with-apple-silicon-a35e39318e97
    docker buildx build \
      --platform linux/amd64,linux/arm64 \
      -t mbari/oni:${VCS_REF} \
      -t mbari/oni:latest \
      --push . \
    && docker pull mbari/oni:${VCS_REF}
else
    sbt 'Docker / publish'
fi