[![Download](https://api.bintray.com/packages/pedjak/gradle-plugins/dockerized-test/images/download.svg) ](https://bintray.com/pedjak/gradle-plugins/dockerized-test/_latestVersion)
[![license](https://img.shields.io/github/license/pedjak/gradle-dockerized-test-plugin.svg)]()

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
            classpath "com.pedjak.gradle.plugins:dockerized-test:0.5"
        }
    }
    
    apply plugin: 'com.github.pedjak.dockerized-test'


The plugin registers to each of test tasks the `docker` extension:

    test {
        docker {
            // base image for creating docker containers that execute the tests
            image = 'foo/testimage' 
            
            // volumes mounted to the containers
            // in a form: host_dir : container_dir
            volumes = [ "/foo/bar": "/foo/bar"] 
            
            // specify the user for starting Gradle test worker 
            // within the container, default to current user
            user = 'root' 
            
            // DockerClient instance responsible for communication
            // with a Docker host
            // If not specified, the plugin creates one using the information
            // available from DOCKER_HOST env variable
            
            // the property can point to a DockerClient instance
            client = new DockerClient(..)
            
            // or to a closure that returns a DockerClient instance
            // the closure is invoked every time when a new test worker is required
            // and the returned client will be used for creating the test worker container
            // This opens up the possibility to spread test execution across a cluster of Docker host
            // using a custom scheduling mechanism
            client = {
                // some logic
                new DockerClient(...)
            }
            
            // the following interceptors can be defined,
            // enabling further customization of the testing process
            
            // invoked right before container creation, 
            // enabling further fine tuning of the passed command
            beforeContainerCreate = { CreateContainerCmd cmd, DockerClient client ->
            }
            
            // invoked right after the container is created
            afterContainerCreate = { String containerId, DockerClient client ->
            }
            
            // invoked right before the container start
            beforeContainerStart = { String containerId, DockerClient client ->
            }
            
            // invoked right after the container start command got executed
            afterContainerStart = { String containerId, DockerClient client ->
            }

            // invoked after the container stops
            // if not specified, the stopped container will be removed
            afterContainerStop = { containerId, client ->
               // you can here copy some file from the container to a central place
            }
            
        }
    }
    
* `docker.image` should point to a Docker image having a JDK installed. A good starting point is 
[Java](https://hub.docker.com/_/java/) set of images and customize
them to your needs if necessary.  If the property is left empty, the tests will be executed
using the standard Gradle mechanism.

* `docker.volumes` is prepopulated so that the docker container mounts:
    * the Gradle home, usually `$HOME/.gradle`
    * the project root
    
* `docker.user` is prepopulated with UID of the the current user.
 
    If not appropriate, you can replace with an another UID.
    
* Finally, if all above options are not serving your needs, you can fully customize
the invocation of the Gradle test worker inside a Docker container by registering
appropriate interceptors. Note that each of them accept up to two parameters, but if the second parameter
is not used in the closure, its declaration can be fully avoided:

       beforeContainerStart = { cmd ->
            cmd.withNetworkMode("foo")
       }

* The plugin uses [docker-java library](https://github.com/docker-java/docker-java) library for the communication with Docker hosts.
Please check its [Javadoc](https://mavenbrowse.pauldoo.com/central/com/github/docker-java/docker-java/3.0.6/docker-java-3.0.6-javadoc.jar/-/index.html)
and [Wiki](https://github.com/docker-java/docker-java/wiki) for the methods available on the arguments passed into the registered interceptors.

* If you want that the plugin uses DockerClient instances created by you, they MUST BE `NettyDockerCmdExecFactory`
based. The factory can be specified as follows:

        DockerClientBuilder.getInstance(DefaultDockerClientConfig.createDefaultConfigBuilder())
                            .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
                            .build()
   
* By setting `maxParallelForks` property on the test task, your tests will be executed in parallel
 
