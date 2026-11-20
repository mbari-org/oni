#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
echo $SCRIPT_DIR
cd "$SCRIPT_DIR"

# Print the value of an sbt setting/task as plain text (last line of output, ANSI codes removed)
sbt_print() {
    { sbt --client "print $1" 2>/dev/null || true; } \
        | perl -pe 's/\e\[[0-9;]*[A-Za-z]//g' \
        | { grep -v '^[[:space:]]*$' || true; } \
        | tail -1
}

ARCH=$(uname -m)
if [[ $ARCH == 'arm64' ]]; then
    # Restart the sbt server so sbt-git picks up any tags created since it started
    sbt --client shutdown > /dev/null 2>&1 || true

    sbt --client 'Docker / stage'

    # Ask sbt where it staged the build. Don't search target/: sbt 2 keeps a separate
    # stage directory per Scala version (target/out/jvm/scala-X.Y.Z/...), so old ones
    # with stale jars linger after a Scala upgrade.
    STAGE_DIR=$(sbt_print 'oni/Docker/stagingDirectory')
    APP_VERSION=$(sbt_print 'oni/version')

    if [[ -z "$STAGE_DIR" || ! -f "$STAGE_DIR/Dockerfile" ]]; then
        echo "ERROR: could not locate Docker staging directory after 'Docker / stage'" >&2
        exit 1
    fi

    VCS_REF=$(git tag | sort -V | grep -E '^[0-9]' | tail -1)

    # Refuse to push an image whose contents don't match the tag it will be published as
    if [[ "$APP_VERSION" != "$VCS_REF" ]]; then
        echo "ERROR: sbt version ($APP_VERSION) does not match latest git tag ($VCS_REF)." >&2
        echo "       Is HEAD tagged and the working tree clean?" >&2
        exit 1
    fi
    if ! find "$STAGE_DIR" -name "org.mbari.oni-${VCS_REF}.jar" | grep -q .; then
        echo "ERROR: $STAGE_DIR does not contain org.mbari.oni-${VCS_REF}.jar" >&2
        exit 1
    fi

    cd "$STAGE_DIR"
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
