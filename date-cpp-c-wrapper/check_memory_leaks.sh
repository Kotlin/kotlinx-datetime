set -ex
common_cxxflags="-std=c++11 -I src/main/public/"
clang++ --analyze $common_cxxflags -DUSE_OS_TZDB=1 -DAUTO_DOWNLOAD=0 -DHAS_REMOTE_API=0 -I ../date-cpp-library/date/include/ src/main/cpp/cdate.cpp
clang++ --analyze $common_cxxflags src/main/cpp/apple.mm
