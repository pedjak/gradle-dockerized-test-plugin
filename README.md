What
====

Gradle plugin for running test within a Docker container.

Why
===

Running functional and integration tests in parallel on a single machine,
testing complexer setups, and better test isolation.

Requirements
============

* [Docker](http://www.docker.com)
* this plugin

Usage
=====



    buildscript {
        repositories {
            maven { url = "http://dl.bintray.com/pedjak/gradle-plugins"}
        }
        dependencies {
            classpath "com.pedjak.gradle.plugins:dockerized-test:0.3"
        }
    }
    
    apply plugin: 'com.github.pedjak.dockerized-test'


The plugin registers to each of test tasks `docker` extension:

    test {
        docker {
            image = 'foo/testimage' // base image for creating docker containers that execute the tests
            
            volumes = [ "/foo/bar": "/foo/bar"] // volumes mounted to the containers
            
            user = 'root' // specify the user for starting Gradle test worker within the container, default to current user
            
            argsInspect = { List args ->
                // custom args processing and tweaking
                // of the docker command starting the testworker inside a container
                // returned args will be used for the final docker container start
                args
            }
        }
    }
    
* `docker.image` should point to a Docker image having a JDK installed. A good starting point is 
[dockerfile/java](https://registry.hub.docker.com/u/dockerfile/java/) set of images and customize
them to your needs if necessary.  If the property is left empty, the tests will be executed
using the standard Gradle mechanism.

* `docker.volumes` is prepopulated so that the docker container mounts:
    * the Gradle home, usually `$HOME/.gradle`
    * the project root
    
* `docker.user` is prepopulated with UID of the the current user.
 
    If not appropriate, you can replace with an another UID.
    
* finally, if all above options are not serving your needs, you can fully customize
the invocation of the Gradle test worker inside a Docker container by registering
a closure to `docker.argsInspect`. It is invoked before the container start, providing
you access to all arguments of `docker` command. The closure *must* return a list of
arguments that is going to be used for the final docker container start.
