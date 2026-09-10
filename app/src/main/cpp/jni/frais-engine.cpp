#include <jni.h>
#include <string>
#include <vector>
#include "../core/FuzzySearch.hpp"
#include "../core/AppClassifier.hpp"

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_khaled_frais_utils_FuzzySearch_nativeSearch(JNIEnv* env, jobject thiz, jstring raw, jstring query, jboolean use_fuzzy) {
    const char* raw_str = env->GetStringUTFChars(raw, nullptr);
    const char* query_str = env->GetStringUTFChars(query, nullptr);

    bool result = frais::FuzzySearch::search(raw_str, query_str, use_fuzzy);

    env->ReleaseStringUTFChars(raw, raw_str);
    env->ReleaseStringUTFChars(query, query_str);

    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_khaled_frais_app_FilterClassifier_nativeClassify(JNIEnv* env, jobject thiz, jstring package_name, jstring label, jint category) {
    const char* pkg_str = env->GetStringUTFChars(package_name, nullptr);
    const char* lbl_str = env->GetStringUTFChars(label, nullptr);

    std::vector<int> tags = frais::AppClassifier::classify(pkg_str, lbl_str, category);

    env->ReleaseStringUTFChars(package_name, pkg_str);
    env->ReleaseStringUTFChars(label, lbl_str);

    jintArray result = env->NewIntArray(tags.size());
    env->SetIntArrayRegion(result, 0, tags.size(), tags.data());

    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_khaled_frais_app_AppInfo_nativeIsGame(JNIEnv* env, jobject thiz, jstring package_name, jint category, jboolean has_game_metadata) {
    const char* pkg_str = env->GetStringUTFChars(package_name, nullptr);

    bool result = frais::AppClassifier::isGame(pkg_str, category, has_game_metadata);

    env->ReleaseStringUTFChars(package_name, pkg_str);

    return result;
}

}
