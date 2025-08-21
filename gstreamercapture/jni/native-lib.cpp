#include <jni.h>
#include <gst/gst.h>
#include <gst/app/gstappsink.h>
#include <gst/video/video.h>
#include <android/log.h>
#include <string>
#include <atomic>
#include <pthread.h>
#include <unistd.h>

#define TAG "GStreamerNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

static JavaVM *javaVM = nullptr;
static jobject javaCallbackObj = nullptr;
static jmethodID onFrameMethodID = nullptr;

static std::atomic_bool isPipelineRunning{false};

static GstElement *pipeline = nullptr;
static GstElement *appsink = nullptr;
static GMainLoop *main_loop = nullptr;

static pthread_t gst_thread;
static pthread_key_t jni_env_key;

static std::string pipeline_url;


static JNIEnv *attach_current_thread() {
    JNIEnv *env;
    JavaVMAttachArgs args{JNI_VERSION_1_6, nullptr, nullptr};
    if (javaVM->AttachCurrentThread(&env, &args) != 0) {
        LOGE("Failed to attach thread to JVM");
        return nullptr;
    }
    pthread_setspecific(jni_env_key, env);
    return env;
}

static void detach_current_thread(void *env) {
    javaVM->DetachCurrentThread();
}

static JNIEnv *get_jni_env() {
    auto *env = static_cast<JNIEnv *>(pthread_getspecific(jni_env_key));
    return env ? env : attach_current_thread();
}

static void send_frame_to_java(GstSample *sample) {
    if (!isPipelineRunning.load()) return;

    JNIEnv *env = get_jni_env();
    if (!env || !javaCallbackObj || !onFrameMethodID) return;

    GstCaps *caps = gst_sample_get_caps(sample);
    if (!caps) return;

    GstVideoInfo info;
    if (!gst_video_info_from_caps(&info, caps)) return;

    int width = GST_VIDEO_INFO_WIDTH(&info);
    int height = GST_VIDEO_INFO_HEIGHT(&info);

    GstBuffer *buffer = gst_sample_get_buffer(sample);
    GstMapInfo map;
    if (!gst_buffer_map(buffer, &map, GST_MAP_READ)) return;

    int ySize = width * height;
    int uSize = ySize / 4;
    int vSize = ySize / 4;

    if (map.size < ySize + uSize + vSize) {
        gst_buffer_unmap(buffer, &map);
        return;
    }

    jbyteArray yPlane = env->NewByteArray(ySize);
    jbyteArray uPlane = env->NewByteArray(uSize);
    jbyteArray vPlane = env->NewByteArray(vSize);

    env->SetByteArrayRegion(yPlane, 0, ySize, reinterpret_cast<const jbyte *>(map.data));
    env->SetByteArrayRegion(uPlane, 0, uSize, reinterpret_cast<const jbyte *>(map.data + ySize));
    env->SetByteArrayRegion(vPlane, 0, vSize, reinterpret_cast<const jbyte *>(map.data + ySize + uSize));

    env->CallVoidMethod(javaCallbackObj, onFrameMethodID, width, height, yPlane, uPlane, vPlane);

    env->DeleteLocalRef(yPlane);
    env->DeleteLocalRef(uPlane);
    env->DeleteLocalRef(vPlane);

    gst_buffer_unmap(buffer, &map);
}

static GstFlowReturn on_new_sample(GstAppSink *sink, gpointer user_data) {
    if (!isPipelineRunning.load()) return GST_FLOW_EOS;
    GstSample *sample = gst_app_sink_pull_sample(sink);
    if (!sample) return GST_FLOW_ERROR;

    send_frame_to_java(sample);
    gst_sample_unref(sample);
    return GST_FLOW_OK;
}

static std::string build_pipeline_string(const std::string &url) {
    const std::string COMMON_SUFFIX = " ! videoconvert ! video/x-raw,format=I420 ! appsink name=mysink emit-signals=true max-buffers=1 drop=true";

    if (url.find("mjpg") != std::string::npos || url.find("mjpeg") != std::string::npos)
        return "souphttpsrc location=" + url + " ! multipartdemux ! jpegdec" + COMMON_SUFFIX;
    if (url.rfind("rtsp://", 0) == 0)
        return "rtspsrc location=" + url + " latency=200 ! decodebin" + COMMON_SUFFIX;
    if (url.rfind("http://", 0) == 0 || url.rfind("https://", 0) == 0)
        return "souphttpsrc location=" + url + " ! decodebin" + COMMON_SUFFIX;

    return "";
}

static void *gst_main_thread(void *arg) {
    attach_current_thread();

    std::string pipeline_str = build_pipeline_string(pipeline_url);
    if (pipeline_str.empty()) {
        LOGE("Invalid pipeline string");
        return nullptr;
    }

    GError *err = nullptr;
    pipeline = gst_parse_launch(pipeline_str.c_str(), &err);
    if (!pipeline || err) {
        LOGE("Failed to create pipeline: %s", err ? err->message : "unknown error");
        if (err) g_error_free(err);
        return nullptr;
    }

    appsink = gst_bin_get_by_name(GST_BIN(pipeline), "mysink");
    gst_app_sink_set_emit_signals(GST_APP_SINK(appsink), TRUE);

    static GstAppSinkCallbacks callbacks = {nullptr, nullptr, on_new_sample};
    gst_app_sink_set_callbacks(GST_APP_SINK(appsink), &callbacks, nullptr, nullptr);

    main_loop = g_main_loop_new(nullptr, FALSE);
    gst_element_set_state(pipeline, GST_STATE_PLAYING);
    LOGD("Pipeline started");
    isPipelineRunning = true;
    g_main_loop_run(main_loop);
    LOGD("GMainLoop exited");

    gst_element_set_state(pipeline, GST_STATE_NULL);
    if (appsink) {
        gst_object_unref(appsink);
        appsink = nullptr;
    }
    if (pipeline) {
        gst_object_unref(pipeline);
        pipeline = nullptr;
    }
    if (main_loop) {
        g_main_loop_unref(main_loop);
        main_loop = nullptr;
    }

    isPipelineRunning = false;
    detach_current_thread(nullptr);
    return nullptr;
}

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    javaVM = vm;
    pthread_key_create(&jni_env_key, detach_current_thread);
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_eyeson_sdk_gstreamercapturer_ByteBufferVideoCapturer_nativeInitPipeline(JNIEnv *env, jobject,
                                                                   jstring jurl, jobject callback) {
    const char *url = env->GetStringUTFChars(jurl, nullptr);
    pipeline_url = url;

    if (javaCallbackObj) env->DeleteGlobalRef(javaCallbackObj);
    javaCallbackObj = env->NewGlobalRef(callback);

    jclass cls = env->GetObjectClass(callback);
    onFrameMethodID = env->GetMethodID(cls, "onFrame", "(II[B[B[B)V");

    gst_init(nullptr, nullptr);

    pthread_create(&gst_thread, nullptr, gst_main_thread, nullptr);
    env->ReleaseStringUTFChars(jurl, url);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_eyeson_sdk_gstreamercapturer_ByteBufferVideoCapturer_nativeStopPipeline(JNIEnv *env, jobject) {
    if (isPipelineRunning.load()) {
        if (main_loop) {
            g_main_loop_quit(main_loop);
        }
        pthread_join(gst_thread, nullptr);
    }

    if (javaCallbackObj) {
        env->DeleteGlobalRef(javaCallbackObj);
        javaCallbackObj = nullptr;
    }
    onFrameMethodID = nullptr;
}
