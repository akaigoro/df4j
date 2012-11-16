This package provides a simple framework for using Erlang Actors in Java.

<<<<<<< HEAD
=======
[![Build Status](https://secure.travis-ci.org/jafl/java_actor.png?branch=master)](http://travis-ci.org/jafl/java_actor)

>>>>>>> 1188866b67ad11c0a9cd12f517d1915e351275e9
Installation
------------

This package is built using Maven (http://maven.apache.org/).  Once you
have installed Maven, run one of the following commands:

  To include unit tests:
    mvn clean install -Dmaven.surefire.debug="-Xmx512m"

  To skip unit tests:
    mvn clean install -Dmaven.test.skip=true

Either command will install the jar into your local maven repository.  The
artifact is com.nps.concurrent:actor.

Usage
-----

Each actor runs in a separate thread and must extend com.nps.concurrent.Actor.

Each actor must implement process() to handle messages.  To retrieve
further messages within process(), call one of the versions of next().  To
send a message to an actor, invoke recv() on that actor.  Ideally, all
actors should have no public functions beyond what is required to
initialize them before the messages start flying, because this eliminates
the need for locks, but this is probably not realistic in a complex system.

Each actor must be configured with an agent:

   * com.nps.concurrent.PersistentThreadAgent
   * com.nps.concurrent.ThreadPoolAgent
   * com.nps.concurrent.JITThreadAgent

Every Actor requires a separate instance of Agent.  For convenience, you
can pass the same Agent to multiple actors, and it will be duplicated for
you.  Note that each actor is expected to install its own MessageFilter, so
this is not copied.

Each ThreadPoolAgent requires that you provide an ActorThreadPool in which
the actor will run.  You can have multiple ActorThreadPools, but each Actor
runs in a single, specific pool.  Actors that are constantly busy should
use PersistentThreadAgent to avoid monopolizing the threads in a pool.  The
size of each ActorThreadPool should be tuned to avoid congestion.  If there
are too few threads, actors may have to wait a long time before they get a
time slice.  To ensure that all messages are eventually processed, actors
must never block waiting for a message.  Instead, they should sleep after
storing internal state describing what to do when the expected message
eventually arrives.

The test cases suggest that ThreadPoolAgent can be faster than
PersistentThreadAgent.  The former may have additional latency since it can
cause actors to wait for an available thread, but having fewer simultaneous
threads appears to be faster, at least in the JVM that I used.
JITThreadActor is significantly slower because thread creation is
expensive, but if you're starved for memory, i.e., you can't use a
persistent thread, and you don't want to wait in a thread pool queue, then
this might be an option, especially if it doesn't receive messages very
often.

To log or monitor messages in the system, you can install MessageSpy
objects.  Each spy gets to see every message that is sent, along with
whether or not is is accepted by the recipient.

Patterns
--------

In standard Java, a shared resource would be protected by a mutex.  Within
the actor paradigm, however, a shared resource is just another actor.  It
accepts messages to atomically modify the resource and return information
about its state.  In this situation, atomic means that the actor will
modify the resource and send a reply before processing any other messages.

The simplest post office is a static function or an object that directly
dispatches messages to a set of actors.  If the senders need to be extra
fast, an actor can be used as the post office, so the dispatching logic is
not executed in the sender's thread.

To implement parallel processing within an actor, i.e., a load balanced
cluster, use java.util.concurrent.ThreadPoolExecutor.

Change Log
----------

1.2  John Lindal 2009-09-19

   * Added MessageSpy to allow logging and monitoring of all messages.
   * Added discussion of behavior patterns.

1.1  John Lindal  2009-09-17

   * Changed the groupId to com.nps.concurrent
   * Refactored SimpleActor into Actor and PersistentThreadAgent.
     Instead of using inheritance, an actor must be passed an agent.
   * Renamed Actor.die() to Actor.retire().
   * Renamed Actor.process() to Actor.act().
   * Actor.act() no longer returns boolean.  Call retire() instead.
   * Fixed appropriate classes to be public.
   * Moved MessageFilter class out of ActorBase.
   * Added get/setMessageFilter() to Agent.
   * Created ThreadPoolAgent for actors that do not require dedicated threads.
   * Created JITPoolAgent for actors that should create a thread on demand.
