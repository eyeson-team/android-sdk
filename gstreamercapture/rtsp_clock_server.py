#!/usr/bin/env python3

import gi
gi.require_version('Gst', '1.0')
gi.require_version('GstRtspServer', '1.0')

from gi.repository import Gst, GstRtspServer, GObject, GLib

Gst.init(None)


class ClockRtspFactory(GstRtspServer.RTSPMediaFactory):
    def __init__(self):
        super().__init__()
        self.set_shared(True)

    def do_create_element(self, url):
        # GStreamer pipeline with live clock overlay
        pipeline = (
            "videotestsrc is-live=true ! "
            "clockoverlay time-format=\"%H:%M:%S\" halignment=right valignment=bottom font-desc=\"Sans, 48\" ! "
            "videoconvert ! "
            "x264enc tune=zerolatency speed-preset=ultrafast ! "
            "rtph264pay name=pay0 pt=96 config-interval=1"
        )
        return Gst.parse_launch(pipeline)


class RtspServer:
    def __init__(self):
        self.server = GstRtspServer.RTSPServer()
        self.server.set_service("8554")  # default RTSP port

        factory = ClockRtspFactory()
        mount_points = self.server.get_mount_points()
        mount_points.add_factory("/test", factory)

        self.server.attach(None)
        print("RTSP stream ready at rtsp://localhost:8554/test")


if __name__ == "__main__":
    server = RtspServer()
    loop = GLib.MainLoop()
    loop.run()
