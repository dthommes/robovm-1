cmake_minimum_required(VERSION 2.8)
include(ExternalProject)

# Parameters to be specified from command line
# OS name
# INSTALL_DIR if required to override

if(NOT DEFINED OS)
    message(FATAL_ERROR "Please specify an os (macosx, linux, windows), e.g. -DOS=linux")
elseif(OS STREQUAL "linux")
  set(LINUX YES)
elseif(OS STREQUAL "macosx")
  set(MACOSX YES)
elseif(OS STREQUAL "windows")
  set(WINDOWS YES)
else()
    message(FATAL_ERROR "Not supported OS ${OS}" )
endif()

if(NOT DEFINED ARCH)
  set(ARCH "x86_64")
  set(X86_64 YES)
elseif (ARCH STREQUAL "x86")
  set(X86 YES)
elseif (ARCH STREQUAL "x86_64")
  set(X86_64 YES)
else()
    message(FATAL_ERROR "Not supported ARCH ${ARCH}")
endif()

if(NOT DEFINED ENV{JAVA_HOME} AND NOT DEFINED JAVA_HOME)
  message(FATAL_ERROR "Please provide env variable JAVA_HOME, e.g. export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64")
endif()

if (WINDOWS)
  if(NOT DEFINED MINGW_VARIANT)
    message(FATAL_ERROR "Please setup MINGW_VARIANT, e.g. -DMINGW_VARIANT=x86_64-w64-mingw32 variable")
  endif()

  # the name of the target operating system
  SET(CMAKE_SYSTEM_NAME Windows)

  # which compilers to use for C and C++
  SET(CMAKE_C_COMPILER ${MINGW_VARIANT}-gcc)
  SET(CMAKE_CXX_COMPILER ${MINGW_VARIANT}-g++)
  SET(CMAKE_RC_COMPILER ${MINGW_VARIANT}-windres)
  # here is the target environment located
  SET(CMAKE_FIND_ROOT_PATH /usr/${MINGW_VARIANT})

  # to override rdynamics
  set(CMAKE_SHARED_LIBRARY_LINK_CXX_FLAGS "")
  set(CMAKE_SHARED_LIBRARY_LINK_C_FLAGS "")

  # adjust the default behaviour of the FIND_XXX() commands:
  # search headers and libraries in the target environment, search
  # programs in the host environment
  set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
  set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
  set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)

  # set variable to build external projects 
  set(TENTATIVE_CROSS_COMPILE_HOST "--host=${MINGW_VARIANT}")
endif()

project(robovm-libimobiledevice)

if (WINDOWS)
  # when system name is set and CMAKE_FIND_ROOT_PATH it will not be able to find java so define includes manualy, 
  # assumpt that host is linux
  set(JAVA_INCLUDE_PATH "$ENV{JAVA_HOME}/include")
  set(JAVA_INCLUDE_PATH2 "$ENV{JAVA_HOME}/include/linux")
  set(JNI_INCLUDE_DIRS "${JAVA_INCLUDE_PATH};${JAVA_INCLUDE_PATH2}")
else()
  find_package(JNI REQUIRED)
endif()

message (STATUS "OS=${OS}")
message (STATUS "ARCH=${ARCH}")
message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")

# set(C_CXX_FLAGS "${C_CXX_FLAGS} -fdata-sections -ffunction-sections -Wno-unused")
if(MACOSX AND X86)
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -arch i386 -mmacosx-version-min=10.6")
  set(LD_FLAGS "${LD_FLAGS} -arch i386 -mmacosx-version-min=10.6")
elseif(MACOSX AND X86_64)
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -arch x86_64 -mmacosx-version-min=10.6")
  set(LD_FLAGS "${LD_FLAGS} -arch x86_64 -mmacosx-version-min=10.6")
elseif(LINUX AND X86)
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -m32 -fPIC")
  set(LD_FLAGS "${LD_FLAGS} -m32")
elseif(LINUX AND X86_64)
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -m64 -fPIC")
  set(LD_FLAGS "${LD_FLAGS} -m64")
elseif(WINDOWS AND X86)
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -m32")
  set(LD_FLAGS "${LD_FLAGS} -m32")
elseif(WINDOWS AND X86_64)
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -m64")
  set(LD_FLAGS "${LD_FLAGS} -m64")
endif()

set(EXTPREFIX "${CMAKE_BINARY_DIR}/ext")
set(C_CXX_FLAGS "${C_CXX_FLAGS} -I${EXTPREFIX}/include -I/usr/local/include")
set(EXTCFLAGS "-I${EXTPREFIX}/include/libusb-1.0 -I${EXTPREFIX}/include/libxml2 ${C_CXX_FLAGS}")
set(LDFLAGS "-L${EXTPREFIX}/lib")

if(MACOSX)
  exec_program(xcode-select ARGS --print-path OUTPUT_VARIABLE XCODE_PATH)
  set(SYSROOT "${XCODE_PATH}/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.13.sdk")
  if(NOT IS_DIRECTORY ${SYSROOT})
    set(SYSROOT "${XCODE_PATH}/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk")
  endif()
  if(NOT IS_DIRECTORY ${SYSROOT})
    message(FATAL_ERROR "No supported MacOSX SDK found")
  endif()
  if(NOT IS_DIRECTORY "/usr/local/opt/openssl/include")
    message(FATAL_ERROR "No OpenSSL found")
  endif()
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -isysroot ${SYSROOT}")
  set(EXTCFLAGS "${EXTCFLAGS} -I/usr/local/opt/openssl/include -isysroot ${SYSROOT}")
  set(LDFLAGS "${LDFLAGS} -L/usr/local/opt/openssl/lib")
endif()

ExternalProject_Add(extlibusbx
  URL https://github.com/libusb/libusb/archive/v1.0.21.zip 
  CONFIGURE_COMMAND bash -c "CFLAGS='${EXTCFLAGS}' PKG_CONFIG_PATH='${EXTPREFIX}/lib/pkgconfig' ./autogen.sh ${TENTATIVE_CROSS_COMPILE_HOST} --enable-shared=no --prefix=${EXTPREFIX} --without-cython"
  BUILD_IN_SOURCE 1
)

# build openssl, required for windows and good to have static for linux
if (WINDOWS)
  # for openssl there are different targets depending on arch
  if (ARCH STREQUAL "x86")
    set(TENTATIVE_OPENSSL_ARCH mingw)
  else()
    set(TENTATIVE_OPENSSL_ARCH mingw64)
  endif()
  # also make a dependency to openssl
  set(TENTATIVE_OPENSSL_DEP extopenssl)

  ExternalProject_Add(extopenssl
    URL https://github.com/openssl/openssl/archive/OpenSSL_1_1_0f.zip
    CONFIGURE_COMMAND bash -c "./Configure ${TENTATIVE_OPENSSL_ARCH} no-shared no-capieng --cross-compile-prefix=${MINGW_VARIANT}- --prefix=${EXTPREFIX} --openssldir=${EXTPREFIX}"
    BUILD_IN_SOURCE 1
  )
elseif (LINUX)
  # some linuxes ship old version of openssl which will cause library not found so it is better to link it staticaly
  # also make a dependency to openssl
  set(TENTATIVE_OPENSSL_DEP extopenssl)

  ExternalProject_Add(extopenssl
    URL https://github.com/openssl/openssl/archive/OpenSSL_1_1_0f.zip
    CONFIGURE_COMMAND bash -c "./Configure linux-${ARCH} no-shared no-capieng no-dso --prefix=${EXTPREFIX} --openssldir=${EXTPREFIX}"
    BUILD_IN_SOURCE 1
  )
endif()
ExternalProject_Add(extlibplist
  URL https://github.com/libimobiledevice/libplist/archive/2.0.0.tar.gz
  CONFIGURE_COMMAND bash -c "CFLAGS='${EXTCFLAGS}' PKG_CONFIG_PATH='${EXTPREFIX}/lib/pkgconfig' ./autogen.sh ${TENTATIVE_CROSS_COMPILE_HOST} --enable-shared=no --enable-udev=no --prefix=${EXTPREFIX} --without-cython" 
  PATCH_COMMAND bash -c "${CMAKE_SOURCE_DIR}/patches/apply-patches"
  BUILD_IN_SOURCE 1
)
ExternalProject_Add(extlibusbmuxd
  URL https://github.com/libimobiledevice/libusbmuxd/archive/1.0.10.zip
  CONFIGURE_COMMAND bash -c "CFLAGS='${EXTCFLAGS}' PKG_CONFIG_PATH='${EXTPREFIX}/lib/pkgconfig' ac_cv_func_realloc_0_nonnull=yes ac_cv_func_malloc_0_nonnull=yes ./autogen.sh ${TENTATIVE_CROSS_COMPILE_HOST} --enable-shared=no --prefix=${EXTPREFIX}"
  BUILD_IN_SOURCE 1
  DEPENDS extlibusbx extlibplist
)



# allow to override repository for quiker checkouts on local builds
if(DEFINED ENV{ROBOVM_LIBIMOBILEDEVICE_GIT_PATH})
	set(LIBIMOBILEDEVICE_GIT_PATH "$ENV{ROBOVM_LIBIMOBILEDEVICE_GIT_PATH}")
	message (STATUS "Using custom libimobiledevice.git @ ${LIBIMOBILEDEVICE_GIT_PATH}")
else()
	set(LIBIMOBILEDEVICE_GIT_PATH https://github.com/libimobiledevice/libimobiledevice.git)
endif()

set(LIBIMOBILEDEVICE_CFLAGS "${EXTCFLAGS}")
if(MACOSX)
  set(LIBIMOBILEDEVICE_CFLAGS "${EXTCFLAGS} -std=gnu89")
endif()
ExternalProject_Add(extlibimobiledevice
  GIT_REPOSITORY ${LIBIMOBILEDEVICE_GIT_PATH}
  GIT_TAG 5a85432719fb3d18027d528f87d2a44b76fd3e12
  CONFIGURE_COMMAND bash -c "PKG_CONFIG_PATH='${EXTPREFIX}/lib/pkgconfig' CFLAGS='${LIBIMOBILEDEVICE_CFLAGS}' LDFLAGS='${LDFLAGS}' ./autogen.sh ${TENTATIVE_CROSS_COMPILE_HOST} --prefix=${EXTPREFIX} --enable-debug-code --enable-shared=no --without-cython"
  UPDATE_COMMAND "" 
  BUILD_IN_SOURCE 1
  DEPENDS ${TENTATIVE_OPENSSL_DEP} extlibplist extlibusbmuxd
)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${C_CXX_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${C_CXX_FLAGS} -fno-rtti")
set(CMAKE_INSTALL_DO_STRIP YES)

include_directories(${JAVA_INCLUDE_PATH} ${JAVA_INCLUDE_PATH2} ${EXTPREFIX}/include)
set(LIBS -limobiledevice -lusbmuxd -lplist -lssl -lcrypto)

if(MACOSX)
  set(LIBS ${LIBS} -liconv)
elseif(WINDOWS)
  set(LLVM_LDFLAGS -static-libgcc -static-libstdc++)
  set(LIBS ${LIBS} -lws2_32)
endif()

set(SRC
  src/main/native/libimobiledevice_wrap.c
)

add_library(robovm-libimobiledevice SHARED ${SRC})
add_dependencies(robovm-libimobiledevice extlibimobiledevice)
target_link_libraries(robovm-libimobiledevice ${LIBS} ${LDFLAGS})
if(LINUX)
  set_target_properties(robovm-libimobiledevice PROPERTIES LINK_FLAGS "-Wl,--gc-sections -Wl,--version-script=${CMAKE_SOURCE_DIR}/src/main/native/exports_linux.sym")
elseif(MACOSX)
  set_target_properties(robovm-libimobiledevice PROPERTIES LINK_FLAGS "-Wl,-dead_strip -Wl,-exported_symbols_list -Wl,${CMAKE_SOURCE_DIR}/src/main/native/exports_macosx.sym")
elseif(WINDOWS)
  set_target_properties(robovm-libimobiledevice PROPERTIES LINK_FLAGS "-static -Wl,--gc-sections -Wl,--kill-at")
endif()

if(DEFINED INSTALL_DIR)
  # replace template with values
  string(REPLACE "_OS_" "${OS}" INSTALL_DIR ${INSTALL_DIR})
  string(REPLACE "_ARCH_" "${ARCH}" INSTALL_DIR ${INSTALL_DIR})
  message (STATUS "Custom install dir=${INSTALL_DIR}")
else(DEFINED INSTALL_DIR)
  set(INSTALL_DIR ${CMAKE_SOURCE_DIR}/src/main/resources/org/robovm/libimobiledevice/binding/${OS}/${ARCH})
endif()

if(WINDOWS)
  install(TARGETS robovm-libimobiledevice RUNTIME DESTINATION ${INSTALL_DIR})
else()
  install(TARGETS robovm-libimobiledevice LIBRARY DESTINATION ${INSTALL_DIR})
endif()
