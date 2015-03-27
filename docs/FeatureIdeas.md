# Feature Ideas

This is a scratchpad for some ideas that have popped into heads, but haven't been worked on yet.

## Metrics

- Support a 'DevOps' Dashboard with Docker Metrics

### Custom Metrics

- Support plugins for custom metrics
    - JVM Statistics
        - jstat -options (http://hg.openjdk.java.net/jdk9/jdk9/jdk/file/d49e247dade6/src/jdk.jcmd/share/classes/sun/tools/jstat/resources/jstat_options)
        - jcmd <pid> PerfCounter.print
        - JMX Numbers
    - RESTful endpoints
    - Other platforms?