Docker Manager
==============

This project was started to take over the Ventiv Technology Environment Manager application.  However, this one will be
more generic, and will support ANY Dockerized project.

Running
-------

In order to run this project, you'll need some configuration, see the Configuring section below.  Generally, you'll want to
simplify this process by storing the configuration in a VCS repository.  That being said, the general process for running is to do the
following:

1. Clone this repository
2. cd DockerManager
3. Clone the configuration repository into 'config'.  E.g.
    git clone http://gitblit.int.aonesolutions.us/gitblit/r/config/DockerManagerConfig.git config
4. When running DockerManagerApplication.groovy, be sure to add your authentication from a spring profile.  E.g.
    --spring.profiles.active=aes


Configuring
-----------

To Be Documented