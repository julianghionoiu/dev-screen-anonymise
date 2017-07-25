[![Java Version](http://img.shields.io/badge/Java-1.8-blue.svg)](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
[![Codeship Status for julianghionoiu/dev-screen-anonymise](https://img.shields.io/codeship/a55a8330-4133-0135-7b7d-4ab391348566/master.svg)](https://codeship.com/projects/230067)
[![Coverage Status](https://coveralls.io/repos/github/julianghionoiu/dev-screen-anonymise/badge.svg?branch=master)](https://coveralls.io/github/julianghionoiu/dev-screen-anonymise?branch=master)

Tool to anonymise a screen recording

# Building

To build, execute

```
./gradlew build shadowJar -i 
```

# Prepare video

Create a folder to store the subimages containing sensitive data, say `subimages`

Go through the video frames and take note of the frames containing sensitive data.  
Dump those frames using FFMpeg:
```bash
ffmpeg -i real-recording.mp4 -ss 00:00:14.000 -vframes 1 subimages/screen.png
```

Using GIMP, crop the areas containing the data you want to mask. Overwrite the original image.

Do a trial run with sub-section of the video:
```bash
ffmpeg -i real-recording.mp4 -ss 52:51 -t 60 rec-1min.mp4
```

# Executing

To anonymize a video, execute

```
java -jar build/libs/dev-screen-anonymise-0.0.1-SNAPSHOT-all.jar \
    --input <input.mp4> \
    --output <outputpath.mp4> \
    --subimages-dir <subimagesdir> \
    --matching-threshold 0.95 \
    --continuous-block-size 3
```

For example

```
java -jar build/libs/dev-screen-anonymise-all.jar --output ./output.mp4 test1/sample-recording.mp4 test1/subimage-2.png
```