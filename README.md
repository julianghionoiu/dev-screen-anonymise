[![Java Version](http://img.shields.io/badge/Java-1.8-blue.svg)](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
[![Codeship Status for julianghionoiu/dev-screen-anonymise](https://img.shields.io/codeship/a55a8330-4133-0135-7b7d-4ab391348566/master.svg)](https://codeship.com/projects/230067)
[![Coverage Status](https://coveralls.io/repos/github/julianghionoiu/dev-screen-anonymise/badge.svg?branch=master)](https://coveralls.io/github/julianghionoiu/dev-screen-anonymise?branch=master)

# Tool to anonymize a screen recording

## Building

To build, execute

```
./gradlew build shadowJar -i 
```


## Concatenate video

If you need to merge two videos, here is how you do it.

Option 1. Concat demuxer
```bash
# mylist.txt
file 'part1.mp4'
file 'part2.mp4'

ffmpeg -f concat -safe 0 -i mylist.txt -c copy real-recording.mp4
```

Option 2. Concat protocol
```bash
ffmpeg -i screencast_20171001T185812.mp4 -i screencast_20171008T151852.mp4 \
       -filter_complex "[0:v:0] [1:v:0] concat=n=2:v=1 [v]" \
       -map "[v]" -c:v libx264 real-recording.mp4
```


## Prepare video

Create a folder to store the subimages containing sensitive data, say `subimages`

Remove the beginning of a video if it contains unnecessary information
```bash
ffmpeg -i real-recording.mp4 -ss 00:12 rec-full.mp4
```

Go through the video frames and take note of the frames containing sensitive data.
Dump those frames using FFMpeg:
```bash
ffmpeg -i rec-full.mp4 -ss 00:00:14.000 -vframes 1 subimages/screen.png
```

Using GIMP, crop the areas containing the data you want to mask. Overwrite the original image.

Do a trial run with sub-section of the video:
```bash
ffmpeg -i rec-full.mp4 -ss 00:00 -t 60 rec-1min.mp4
```


## Executing

To anonymize a video, execute

```
java -jar build/libs/dev-screen-anonymise-0.0.1-SNAPSHOT-all.jar \
    --input ./xyz/input.mp4 \
    --output ./xyz/output.mp4 \
    --subimages-dir ./xyz/subimages\
    --matching-threshold 0.85 \
    --continuous-block-size 7
```

For example

```
java -jar build/libs/dev-screen-anonymise-all.jar --output ./output.mp4 test1/sample-recording.mp4 test1/subimage-2.png
```