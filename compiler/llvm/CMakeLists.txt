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

  SET(TENTATIVE_WIN32_FLAGS "-DWIN32=true" "-DCROSS_TOOLCHAIN_FLAGS_NATIVE=-DCMAKE_TOOLCHAIN_FILE=${CMAKE_SOURCE_DIR}/host_linux.cmake")

  # to override rdynamics
  set(CMAKE_SHARED_LIBRARY_LINK_CXX_FLAGS "")
  set(CMAKE_SHARED_LIBRARY_LINK_C_FLAGS "")

  # adjust the default behaviour of the FIND_XXX() commands:
  # search headers and libraries in the target environment, search
  # programs in the host environment
  set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
  set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
  set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
endif()

project(robovm-llvm)

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

set(C_CXX_FLAGS "${C_CXX_FLAGS} -fvisibility=hidden -fdata-sections -ffunction-sections")
if(MACOSX AND X86_64)
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -arch x86_64 -mmacosx-version-min=10.7")
  set(LD_FLAGS "${LD_FLAGS} -arch x86_64 -mmacosx-version-min=10.7")
elseif(LINUX AND X86)
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -m32")
  set(LD_FLAGS "${LD_FLAGS} -m64")
elseif(LINUX AND X86_64)
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -m64")
  set(LD_FLAGS "${LD_FLAGS} -m64")
elseif(WINDOWS AND X86)
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -m32")
  set(LD_FLAGS "${LD_FLAGS} -m32")
elseif(WINDOWS AND X86_64)
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -m64")
  set(LD_FLAGS "${LD_FLAGS} -m64")
endif()

if(MACOSX)
  exec_program(xcode-select ARGS --print-path OUTPUT_VARIABLE XCODE_PATH)
  set(SYSROOT "${XCODE_PATH}/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.13.sdk")
  if(NOT IS_DIRECTORY ${SYSROOT})
    set(SYSROOT "${XCODE_PATH}/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk")
  endif()
  if(NOT IS_DIRECTORY ${SYSROOT})
    message(FATAL_ERROR "No supported MacOSX SDK found")
  endif()
  set(C_CXX_FLAGS "${C_CXX_FLAGS} -isysroot ${SYSROOT}")
endif()


# allow to override repository for quiker checkouts on local builds
if(DEFINED ENV{ROBOVM_CLANG_GIT_PATH})
  set(CLANG_GIT_PATH "$ENV{ROBOVM_CLANG_GIT_PATH}")
  message (STATUS "Using custom clang.git @ ${CLANG_GIT_PATH}")
else()
  set(CLANG_GIT_PATH git://github.com/robovm/clang.git)
endif()

ExternalProject_Add(extclang
  GIT_REPOSITORY ${CLANG_GIT_PATH}
  GIT_TAG 7b389564897bc6c0431c42f8f1825f04624ff4e3
  UPDATE_COMMAND ""
  CONFIGURE_COMMAND ""
  BUILD_COMMAND ""
  INSTALL_COMMAND ""
  BUILD_IN_SOURCE 1
)


set(LLVM_ARCHS "X86^^ARM^^AArch64")
set(LLVM_C_FLAGS "${C_CXX_FLAGS} -fvisibility=hidden -fdata-sections -ffunction-sections")
set(LLVM_CXX_FLAGS "${C_CXX_FLAGS} -fvisibility=hidden -fdata-sections -ffunction-sections")
if (WINDOWS)
  set(LLVM_CXX_FLAGS "${LLVM_CXX_FLAGS} -Wa,-mbig-obj")
endif()

# allow to override repository for quiker checkouts on local builds
if(DEFINED ENV{ROBOVM_LLVM_GIT_PATH})
  set(LLVM_GIT_PATH "$ENV{ROBOVM_LLVM_GIT_PATH}")
  message (STATUS "Using custom llvm.git @ ${LLVM_GIT_PATH}")
else()
  set(LLVM_GIT_PATH git://github.com/robovm/llvm.git)
endif()

# optimization breaks code execution which caused NPE when working with ARM64 target
if(NOT MACOSX)
       set(TENTATIVE_DISABLE_OPT "-DCMAKE_CXX_FLAGS_RELEASE=-O0 -DNDEBUG")
endif()

ExternalProject_Add(extllvm
  DEPENDS extclang
  GIT_REPOSITORY ${LLVM_GIT_PATH}
  GIT_TAG aceb47b80838681fe65a1d01aac7aae7b863ac5a
  LIST_SEPARATOR ^^
  CONFIGURE_COMMAND cmake -DCMAKE_SHARED_LIBRARY_LINK_CXX_FLAGS=${CMAKE_SHARED_LIBRARY_LINK_CXX_FLAGS}
                          -DCMAKE_SYSTEM_NAME=${CMAKE_SYSTEM_NAME}
                          ${TENTATIVE_WIN32_FLAGS}
                          -DCMAKE_CXX_COMPILER=${CMAKE_CXX_COMPILER}
                          -DCMAKE_C_COMPILER=${CMAKE_C_COMPILER}
                          -DCMAKE_RC_COMPILER=${CMAKE_RC_COMPILER}
                          -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}
                          -DBUILD_SHARED_LIBS=OFF
                          "-DCMAKE_INSTALL_PREFIX=${CMAKE_BINARY_DIR}/llvm"
                          "-DLLVM_TARGETS_TO_BUILD=${LLVM_ARCHS}"
                          "-DCMAKE_C_FLAGS=${LLVM_C_FLAGS}"
                          "-DCMAKE_CXX_FLAGS=${LLVM_CXX_FLAGS}"
                          ${TENTATIVE_DISABLE_OPT}
                          -DLLVM_ENABLE_LIBCXX=YES
                          -DLLVM_ENABLE_CXX1Y=YES
                          -DLLVM_ENABLE_THREADS=YES
                          -DLIBCLANG_BUILD_STATIC=YES
                          -DLLVM_INCLUDE_EXAMPLES=OFF
                          -DLLVM_INCLUDE_TESTS=OFF
                          -DLLVM_INCLUDE_DOCS=OFF
                          ../extllvm/
  PATCH_COMMAND bash -c "rm -rf tools/clang && cp -r ../../../extclang-prefix/src/extclang/ tools/clang/ && ${CMAKE_SOURCE_DIR}/patches/apply-patches"
)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${C_CXX_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${C_CXX_FLAGS} -fno-rtti -std=c++0x")
set(CMAKE_INSTALL_DO_STRIP YES)
if(MACOSX)
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -stdlib=libc++")
endif()

add_definitions(-D__STDC_LIMIT_MACROS -D__STDC_CONSTANT_MACROS)
include_directories(${JAVA_INCLUDE_PATH} ${JAVA_INCLUDE_PATH2} ${CMAKE_BINARY_DIR}/llvm/include)

# This was generated using 'llvm-config --libs core bitreader bitwriter irreader ipo arm x86 aarch64 debuginfo linker option instrumentation objcarcopts'
set(LLVM_LIBS
  -lLLVMObjCARCOpts -lLLVMInstrumentation -lLLVMOption -lLLVMLinker
  -lLLVMDebugInfo -lLLVMAArch64Disassembler -lLLVMAArch64CodeGen
  -lLLVMAArch64AsmParser -lLLVMAArch64Desc -lLLVMAArch64Info
  -lLLVMAArch64AsmPrinter -lLLVMAArch64Utils -lLLVMX86Disassembler
  -lLLVMX86AsmParser -lLLVMX86CodeGen -lLLVMX86Desc -lLLVMX86Info
  -lLLVMX86AsmPrinter -lLLVMX86Utils -lLLVMARMDisassembler -lLLVMARMCodeGen
  -lLLVMSelectionDAG -lLLVMAsmPrinter -lLLVMCodeGen -lLLVMARMAsmParser
  -lLLVMARMDesc -lLLVMMCDisassembler -lLLVMARMInfo -lLLVMARMAsmPrinter
  -lLLVMipo -lLLVMVectorize -lLLVMScalarOpts -lLLVMProfileData -lLLVMObject
  -lLLVMMCParser -lLLVMInstCombine -lLLVMTransformUtils -lLLVMipa
  -lLLVMAnalysis -lLLVMTarget -lLLVMMC -lLLVMIRReader -lLLVMAsmParser
  -lLLVMBitWriter -lLLVMBitReader -lLLVMCore -lLLVMSupport
)
# Required clang libs
if(LINUX)
  # With the GNU linker library order is important and some libraries have
  # to be repeated in order to resolve all symbols properly.
  set(LLVM_LIBS
    -lclangBasic -lclangDriver -lclangFrontend -lclangCodeGen
    -lclangFrontend -lclangDriver -lclangParse -lclangSerialization
    -lclangSema -lclangEdit -lclangAnalysis -lclangAST -lclangLex
    -lclangBasic ${LLVM_LIBS}
  )
elseif(WINDOWS)
  set(LLVM_LIBS
    -lclangBasic -lclangDriver -lclangFrontend -lclangCodeGen
    -lclangFrontend -lclangDriver -lclangParse -lclangSerialization
    -lclangSema -lclangEdit -lclangAnalysis -lclangAST -lclangLex
    -lclangBasic -ldbghelp ${LLVM_LIBS}
  )
elseif(MACOSX)
  set(LLVM_LIBS ${LLVM_LIBS}
    -lclangFrontend -lclangAST -lclangBasic -lclangLex -lclangSerialization
    -lclangSema -lclangEdit -lclangCodeGen -lclangParse -lclangAnalysis
    -lclangDriver
  )
endif()

# This was generated using 'llvm-config --ldflags'
if(LINUX)
  set(LLVM_LDFLAGS
    -L"${CMAKE_BINARY_DIR}/llvm/lib"
    -lrt -ldl -lpthread -lz
  )
elseif(WINDOWS)
  set(LLVM_LDFLAGS
    -L"${CMAKE_BINARY_DIR}/llvm/lib"
    -ldbghelp -static-libgcc -static-libstdc++
  )
elseif(MACOSX)
  set(LLVM_LDFLAGS
    -L"${CMAKE_BINARY_DIR}/llvm/lib"
    -lcurses -lpthread -lz
  )
endif()

set(SRC
  src/main/native/LLVM_wrap.c
  src/main/native/LLVMExtra.cpp
  src/main/native/ClangExtra.cpp
)

add_library(robovm-llvm SHARED ${SRC})
add_dependencies(robovm-llvm extllvm)
target_link_libraries(robovm-llvm ${LLVM_LIBS} ${LLVM_LDFLAGS})
set_target_properties(robovm-llvm PROPERTIES LINKER_LANGUAGE CXX)
if(LINUX)
  set_target_properties(robovm-llvm PROPERTIES LINK_FLAGS "-Wl,--gc-sections -Wl,--version-script=${CMAKE_SOURCE_DIR}/src/main/native/exports_linux.sym")
elseif(MACOSX)
  set_target_properties(robovm-llvm PROPERTIES LINK_FLAGS "-Wl,-dead_strip -Wl,-exported_symbols_list -Wl,${CMAKE_SOURCE_DIR}/src/main/native/exports_macosx.sym")
elseif(WINDOWS)
  set_target_properties(robovm-llvm PROPERTIES LINK_FLAGS "-static -Wl,--gc-sections -Wl,--kill-at")
endif()

if(DEFINED INSTALL_DIR)
  # replace template with values
  string(REPLACE "_OS_" "${OS}" INSTALL_DIR ${INSTALL_DIR})
  string(REPLACE "_ARCH_" "${ARCH}" INSTALL_DIR ${INSTALL_DIR})
  message (STATUS "Custom install dir=${INSTALL_DIR}")
else(DEFINED INSTALL_DIR)
  set(INSTALL_DIR ${CMAKE_SOURCE_DIR}/src/main/resources/org/robovm/llvm/binding/${OS}/${ARCH})
endif()

if(WINDOWS)
  install(TARGETS robovm-llvm RUNTIME DESTINATION ${INSTALL_DIR})
else()
  install(TARGETS robovm-llvm LIBRARY DESTINATION ${INSTALL_DIR})
endif()
