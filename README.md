# Floppy Ears demo

## What's in this repo?

### Slides
The slides for the [NEScala 2020 presentation](./NEScala%202020.pdf).

The slides for the [Chicago Area Scala Meetup June 25, 2020 presentation](./CASE_2020-06-25.pdf).

### Floppy Ears
The library and sbt build plugin to integrate with the fictional external data backend which we will simulate with [Wiremock](http://wiremock.org). See the separate [readme](floppy-ears/README.md) for the library.

_My Mom used to call me "Floppy Ears" when I was a kid when I listened in on conversations...and apparently the name started with my grandmother. Since wiretapping is what we are doing here, I thought this name worked well!_

### Recipe Box
A Play 2.8 example web service example which we will use to demonstrate data reporting with Floppy Ears.

_I wrote this for my brother Mark because I promised him I'd create a recipe web service 20 years ago so we could all access Mom's recipes as we moved all over the country._ :)

### Post Samples
Some recipes you can post into the service. They are pre-populated in the service (by virtue of the [datastore](recipe-box/src/main/datastore/recipes) subdirectory). The service uses simple file IO.

### Wiremock
Run wiremock from [this subdirectory](wiremock) to pick up the request mappings needed to integrate with the fictional data backend. We are using the response tempating feature of Wiremock.

## Demo

1. Run wiremock

First, download wiremock. Run it from the `wiremock` subdirectory.

```
$ cd wiremock
$ java -jar ./wiremock-standalone-2.26.3.jar --global-response-templating --verbose
```

2. Build floppy-ears.


```
$ cd floppy-ears
$ sbt
> ;clean;+compile;+test;scripted;+publishLocal
```

3. Build and run the recipe box.

```
$ cd recipe-box
$ sbt
>;clean;compile;run 
```

4. Call endpoints:

**Search**
GET (http://localhost:9000/rest/v1/recipes)

**Get a recipe (recipe id 1)**
GET (http://localhost:9000/rest/v1/recipes/1)

**Save a recipe**
POST (http://localhost:9000/rest/v1/recipes)

You will see the console for Recipe Box (and Wiremock) show schema registration and events.

