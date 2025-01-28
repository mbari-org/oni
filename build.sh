it#!/usr/bin/env bash

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
echo $SCRIPT_DIR

ARCH=$(uname -m)
if [[ $ARCH == 'arm64' ]]; then
    sbt 'Docker / stage'
    cd $SCRIPT_DIR/oni/target/docker/stage
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