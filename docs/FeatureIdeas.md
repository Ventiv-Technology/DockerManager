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
    
## Better Jenkins Integration

- While a build is running, supply a link to the build console

## Better Feedback to User

- Errors in Build / Deploy need to be presented to user and what they can do to fix the issue
- Deployment needs feedback similar to build
    - Pull status
    - How many steps there are to go