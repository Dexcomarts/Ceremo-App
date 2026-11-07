FROM ubuntu:24.04
RUN apt-get update \
    && apt-get install --no-install-recommends -y ca-certificates curl jq \
    && groupadd --gid 10001 ceremo \
    && useradd --uid 10001 --gid ceremo --create-home --shell /bin/bash ceremo \
    && install -d -o ceremo -g ceremo -m 0700 /tokens \
    && rm -rf /var/lib/apt/lists/*
COPY --chown=ceremo:ceremo --chmod=0755 sonar/sonar_generate_token.sh /usr/local/bin/sonar_generate_token
USER ceremo
ENTRYPOINT ["/usr/local/bin/sonar_generate_token"]
