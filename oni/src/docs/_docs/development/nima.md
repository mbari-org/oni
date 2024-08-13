# nima

Oni was originally written using Helidon's Webserver (aka Nima) to serve the web application. Currently, Oni uses Vert.X. The notes below are in case we switch back to Nima.

## Supporting gzip

From [Slack](https://helidon.slack.com/archives/CCS216A5A/p1719928229654769?thread_ts=1719792358.413759&cid=CCS216A5A)

ne way (and probably the easiest way) would be to simply add the following dependencies. These will be automatically discovered and used. It is not needed to do anything else.

```xml
<!-- gzip -->
<dependency>
    <groupId>io.helidon.http.encoding</groupId>
    <artifactId>helidon-http-encoding-gzip</artifactId>
</dependency>
<!-- deflate -->
<dependency>
    <groupId>io.helidon.http.encoding</groupId>
    <artifactId>helidon-http-encoding-deflate</artifactId>
</dependency>
```

Other way, if you would not want it to be automatically discovered and you would want to do it programatically yourselft would be to use something like this:

```java
WebServer.builder()
    .contentEncoding(encodingBuilder -> encodingBuilder.contentEncodingsDiscoverServices(false)
    .addContentEncoding(GzipEncoding.create())
    .addContentEncoding(DeflateEncoding.create()))
```
But even for that you need the above dependencies.
