[![Java Version](http://img.shields.io/badge/Java-1.8-blue.svg)](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
[![Codeship Status for julianghionoiu/dev-screen-anonymise](https://img.shields.io/codeship/a55a8330-4133-0135-7b7d-4ab391348566/master.svg)](https://codeship.com/projects/230067)
[![Coverage Status](https://coveralls.io/repos/github/julianghionoiu/dev-screen-anonymise/badge.svg?branch=master)](https://coveralls.io/github/julianghionoiu/dev-screen-anonymise?branch=master)

Tool to anonymise a screen recording

# Building

To build, execute

```
./gradlew build shadowJar
```

# Executing

To anonymize a video, execute

```
java -jar build/libs/dev-screen-anonymise-all.jar [--output <outputpath>] \
    <input mp4> <subimage [subimage  ..]>
```

For example

```
java -jar build/libs/dev-screen-anonymise-all.jar --output ./output.mp4 test1/sample-recording.mp4 test1/subimage-2.png
```