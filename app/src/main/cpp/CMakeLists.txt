cmake_minimum_required(VERSION 3.22.1)

project("frais-engine")

add_library(
        frais-engine
        SHARED
        jni/frais-engine.cpp
        core/FuzzySearch.cpp
        core/AppClassifier.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        frais-engine
        ${log-lib}
)