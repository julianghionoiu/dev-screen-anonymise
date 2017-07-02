# dev-screen-anonymise
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