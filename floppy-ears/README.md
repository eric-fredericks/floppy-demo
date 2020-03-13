# Floppy Ears - Endpoint Instrumentation Automation for Data Reporting

A library and tooling for automatic instrumentation of the endpoints of a Play app (microservice) for data reporting.

## Components

### floppy-ears-wiretap
A library with annotations and common code useful for instrumenting web service endpoints to achieve Floppy Ears integration. The library provides:
- An extended set of annotations that you can use to annotate your controller traits indicating that you want support for Floppy Ears instrumentation on the endpoints they represent.
- `FloppyEarsActionFunction`, an implementation of a Play `ActionFunction` and supporting code that you can compose with your action function.
  - The `FloppyEarsActionFunction` registers an Avro schema when your application starts.
  - It also asynchronously reports data, including request bodies, response bodies, url path parameters, and query string parameters flowing through an endpoint.
  - The data is tagged with your user's User ID and a Session ID. The `FloppyEarsContext` provides you with the ability to provide functions to get these pieces of information from a request object.

The end result is that you get your web service endpoints instrumented automatically by using declarative static annotations and calling a few functions when instantiating a controller class in your Play application.

### floppy-ears-plugin
An SBT plugin that generates code to enable integration with Floppy Ears.
- The generator will analyze your code, processing the annotations mentioned above, and generate code that provides supporting code you need to compose your action function with a `FloppyEarsActionFunction`.

## Usage

1. Add the library dependency to your project.

    ```
    libraryDependencies += "org.kalergic.floppyears" %% "floppy-ears-wiretap" % "x.y.z"
    ```

2. Add the sbt plugin to your project.

    `project/plugins.sbt`
    
    ````
    addSbtPlugin("org.kalergic.floppyears" %% "floppy-ears-wiretap" % "x.y.z")
    
    ````
   
3. Annotate your code and add appropriate JSON formatting support.

    - Add the extended annotations to get the code generator going. It generates a Floppy Ears event case class and supporting code for every intercepted method. **You must use these on _controller traits_.**
      - Use `@Wiretap` to designate a method as having intercept support.
        - Use the `majorVersion` parameter to indicate the major version to use when registering the schema.
        - **Important: The parameter names in the controller trait methods must match the parameter names used in the Play routes file.**  
      - Use `@WiretapRequest` if there is a request body, or a transformed version thereof, you want to report in the Floppy Ears event.
        - If your code invokes the `async` method with a `BodyParser[T]` (first parameter list), you should flag `parseJson=false` in this annotation. An example of what this would look like in your controller implementation can be seen [here](plugin/src/sbt-test/floppy-ears-plugin/simple/src/main/scala/org/kalergic/floppyears/plugintest/SampleControllerImpl.scala#L87).
        - The default behavior (`parseJson=true`) assumes you are parsing the JSON in the `block` function you pass to `async` (second parameter list) and that the request body needs to be cast to `AnyContentAsJson` and passed to the JSON parser to get it into the shape of your type `T` for the Floppy Ears event.
        - The library only supports JSON content at this time.
      - Use `@WiretapRequestTransform` if you wish to have the request body transformed in some custom, application-defined way.
        - If you do not use this annotation, the type specified in `@WiretapRequest` is used for the request body type in the Floppy Ears event class.
        - If you use this annotation, the class specified here is used for the request type in the generated Floppy Ears event class, and the generated code will invoke the specified function to transform the request body to the target type.
        - If you use this annotation and you do not use `@WiretapRequest`, the generator will produce an error.
      - Use `@WiretapResponse` if there is a response body, or a transformed version thereof, you want to report in the Floppy Ears event.
      - Use `@WiretapResponseTransform` if you wish to have the response body transformed in some custom, application-defined way.
        - If you do not use this annotation, the type specified in `@WiretapResponse` is used for the response body type in the Floppy Ears event class.
        - If you use this annotation, the class specified here is used for the response type in the generated Floppy Ears event class, and the generated code will invoke the specified function to transform the response body to the target type.
        - If you use this annotation and you do not use `@WiretapResponse`, the generator will produce an error.
      - Use `@WiretapIgnore` if you wish to suppress a controller method parameter from the Flo event case class.
      - Use `@WiretapConvert` if you need to define a custom parameter conversion function. The function must be of type `String => A` where `A` is the type of the parameter.
        - This is not necessary for value types or strings - the code generator can handle those types automatically.
        - The code generator generates code that handles `Option`s for you.
     - This library uses `play-json` to deserialize your request and response objects, but `json4s` to _reserialize_ for the Floppy Ears backend.

5. Build your project.

    The plugin will analyze your code and generate support for you to compose the `FloppyEarsActionFunction` into your action functions.

6. Implement your app-specific [FloppyEarsContext](wiretap/src/main/scala/org/kalergic/floppyears/wiretap/FloppyEarsContext.scala).

    Example [here](wiretap/src/test/scala/org/kalergic/floppyears/wiretap/MyFloppyEarsContext.scala).

7. Update your controller.

    - Inject your `FloppyEarsContext` into your controller implementation (via Macwire, Guice, etc.). It's probably best to make it an implicit constructor parameter.
    - Compose the `FloppyEarsActionFunction` with yours by calling the helper method `withFloppyEars` on the [companion object](wiretap/src/main/scala/org/kalergic/floppyears/wiretap/FloppyEarsActionFunction.scala#L78).

    Example of how to use the helper method [here](plugin/src/sbt-test/floppy-ears-plugin/simple/src/main/scala/org/kalergic/floppyears/plugintest/SampleControllerImpl.scala).

    **Note:** You will need to re-use your action function and declare it as an instance variable in your controller as the `FloppyEarsActionFunction` is designed to be composed with yours at controller constructor execution time.

8. Configure the `floppy-ears` execution context.

    - We want all data reporting to occur asynchronously and via an execution context that is not the default Play execution context.
    - This library requires you to configure a dispatcher named `floppy-ears`.
      - _You might want to avoid using defaults. See the [Play Framework documentation](https://www.playframework.com/documentation/2.5.x/ThreadPools#Using-other-thread-pools) and the [Akka documentation](https://doc.akka.io/docs/akka/current/dispatchers.html#setting-the-dispatcher-for-an-actor) for details._

    Assuming you pass the Play actor system's `actorSystem.dispatchers` in your `FloppyEarsContext` instance, you need to update your `application.conf` file with the `floppy-ears` dispatcher. Here's an example (this is not battle-tested):

    ```
    floppy-ears {
      type = Dispatcher

      # What kind of ExecutionService to use
      executor = "fork-join-executor"

      # Configuration for the fork join pool
      fork-join-executor {
        # Min number of threads to cap factor-based parallelism number to
        parallelism-min = 1
        # Parallelism (threads) ... ceil(available processors * factor)
        parallelism-factor = 2.0
        # Max number of threads to cap factor-based parallelism number to
        parallelism-max = 10
      }
      # Throughput defines the maximum number of messages to be
      # processed per actor before the thread jumps to the next actor.
      # Set to 1 for as fair as possible.
      throughput = 1
    }  
    ```
    See the [Akka documentation](https://doc.akka.io/docs/akka/current/dispatchers.html) for details on your configuration options.


### Enjoy the separation of concerns and boilerplate elimination!
