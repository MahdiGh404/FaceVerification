#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

// ncnn
#include "net.h"
//#include "benchmark.h"

static ncnn::UnlockedPoolAllocator g_blob_pool_allocator;
static ncnn::PoolAllocator g_workspace_pool_allocator;
static ncnn::Net m2net;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "M2Ncnn", "JNI_OnLoad");
    ncnn::create_gpu_instance();
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "M2Ncnn", "JNI_OnUnload");
    ncnn::destroy_gpu_instance();
}

// public native boolean Init(AssetManager mgr);
JNIEXPORT jboolean JNICALL
Java_ir_blue_1saffron_fv_M2Ncnn_Init(JNIEnv *env, jobject thiz, jobject assetManager) {
    ncnn::Option opt;
    opt.lightmode = true;
    opt.num_threads = 4;
    opt.blob_allocator = &g_blob_pool_allocator;
    opt.workspace_allocator = &g_workspace_pool_allocator;

    // use vulkan compute
    if (ncnn::get_gpu_count() != 0)
        opt.use_vulkan_compute = true;

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
    m2net.opt = opt;
    // init param
    {
        int ret = m2net.load_param(mgr, "m2-6762729-ncnn.param");
        if (ret != 0) {
            __android_log_print(ANDROID_LOG_DEBUG, "M2Ncnn", "load_param_bin failed");
            return JNI_FALSE;
        }
    }
    // init bin
    {
        int ret = m2net.load_model(mgr, "m2-6762729-ncnn.bin");
        if (ret != 0) {
            __android_log_print(ANDROID_LOG_DEBUG, "M2Ncnn", "load_model failed");
            return JNI_FALSE;
        }
    }

    return JNI_TRUE;
}

// public native float[] Detect(Bitmap bitmap, boolean use_gpu);
JNIEXPORT jfloatArray JNICALL
Java_ir_blue_1saffron_fv_M2Ncnn_Detect(JNIEnv *env, jobject thiz, jobject bitmap,
                                       jboolean use_gpu) {
    if (use_gpu == JNI_TRUE && ncnn::get_gpu_count() == 0) {
        __android_log_print(ANDROID_LOG_DEBUG, "M2Ncnn", "gpu count %d", ncnn::get_gpu_count());
    }
    // double start_time = ncnn::get_current_time();

    /*AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    int width = info.width;
    int height = info.height;
    if (width != 227 || height != 227)
        return NULL;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
        return NULL;*/

    // ncnn from bitmap
    ncnn::Mat in = ncnn::Mat::from_android_bitmap(env, bitmap, ncnn::Mat::PIXEL_BGR);
    // ncnn::Mat in = ncnn::Mat::from_android_bitmap_resize(env, bitmap, ncnn::Mat::PIXEL_BGR, 112,112);

    // m2net
    std::vector<float> cls_scores;
    jfloatArray result;
    {
        ncnn::Mat out;
//        double start_time = ncnn::get_current_time();
//        for (int t = 0; t < 100; t++) {
        ncnn::Extractor ex = m2net.create_extractor();
        ex.set_vulkan_compute(use_gpu);
        ex.input("data", in);
        ex.extract("Embedding_fc1", out);
//        }
//        double elasped = ncnn::get_current_time() - start_time;
//        __android_log_print(ANDROID_LOG_DEBUG, "M2Ncnn", "%.2fms   detect", elasped/100);
        // __android_log_print(ANDROID_LOG_DEBUG, "M2Ncnn", "w %d", out.w);
        cls_scores.resize(out.w);
        jfloat data[out.w];
        result = env->NewFloatArray(out.w);
        for (int j = 0; j < out.w; j++) {
            data[j] = out[j];
        }
        env->SetFloatArrayRegion(result, 0, out.w, data);
    }
    return result;
}

}
