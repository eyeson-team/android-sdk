# Eyeson Android SDK GStreamer add-on 

This feature allows you to add an RTSP stream as a local video source.

## Local RTSP stream for testing 

Launch [rtsp_clock_server.py](rtsp_clock_server.py) to start a local RTSP stream on `rtsp://localhost:8554/test`. This stream will be accessible from the emulator on `rtsp://10.0.2.2:8554/test`. See [here](https://developer.android.com/studio/run/emulator-networking) for further information about the Android Emulator networking setup. 