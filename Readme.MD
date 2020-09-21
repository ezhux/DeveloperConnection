# Developers Connected API

Check if two developers are connected on Github and Twitter. They are connected if they follow each other on Twitter 
and have common Organizations on Github

The HTTP server part is done using Akka-http

For communication with Twitter I'm using the *twitter4s* lib. This is not ideal, because *twitter4s* uses a separate 
(classic) AKKA ActorSystem, whereas akka-http server created another ActorSystem, but a typed one. It would be better 
to re-use the same ActorSystem. 

For communication with Github I've opted for calling the REST API directly and unmarshalling results, because it was 
just easier than using a library. Scala libs for Github that I found require using Github API credentials, but those 
endpoints are public (but are rate limited more strictly).

I'm using *Cats Validated* (https://typelevel.org/cats/datatypes/validated.html) datatype for combining potential errors
 into a List of Errors. The main advantage of this datatype is that it doesn't short-circuit after finding the first 
 Error but allows to collect all Errors.
 
Tests perform actual requests to Twitter and Github and thus require Twitter API credentials in the application.conf 
even for running tests. Maybe I should have mocked the responses...

## Running the app

First you need to put your Twitter Developer keys in application.conf file

In one terminal window run
`sbt run`. A server will be started on port 8080.   

In another terminal window run e.g. curl commands, here are a few sample requests:

`curl http:/localhost:8080/developers/connected/abc/bcd`

`curl http:/localhost:8080/developers/connected/renatocaval/patriknw`

## Running tests

Make sure your Twitter Developer keys are in application.conf

`sbt test`

