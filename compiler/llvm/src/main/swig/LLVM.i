%module LLVM
%{
#include <llvm-c/Core.h>
#include <llvm-c/BitReader.h>
#include <llvm-c/BitWriter.h>
#include <llvm-c/Object.h>
#include <llvm-c/Transforms/IPO.h>
#include <llvm-c/Transforms/PassManagerBuilder.h>
#include <llvm-c/Transforms/Scalar.h>
#include <llvm-c/Transforms/Vectorize.h>
#include <llvm-c/Transforms/Utils.h>
#include <llvm-c/Target.h>
#include <llvm-c/TargetMachine.h>
#include <llvm-c/Linker.h>
#include "../native/LLVMExtra.h"
#include "../native/ClangExtra.h"
%}


%include "LLVMCommon.i"


//
// Registering pointers to opaque structs
//
REF_CLASS(LLVMBasicBlockRef, BasicBlockRef)
REF_CLASS(LLVMBuilderRef, BuilderRef)
REF_CLASS(LLVMContextRef, ContextRef)
REF_CLASS(LLVMDiagnosticInfoRef, DiagnosticInfoRef)
REF_CLASS(LLVMMemoryBufferRef, MemoryBufferRef)
REF_CLASS(LLVMModuleProviderRef, ModuleProviderRef)
REF_CLASS(LLVMModuleRef, ModuleRef)
REF_CLASS(LLVMPassManagerBuilderRef, PassManagerBuilderRef)
REF_CLASS(LLVMPassManagerRef, PassManagerRef)
REF_CLASS(LLVMObjectFileRef, ObjectFileRef)
REF_CLASS(LLVMPassRegistryRef, PassRegistryRef)
REF_CLASS(LLVMTargetDataRef, TargetDataRef)
REF_CLASS(LLVMTargetLibraryInfoRef, TargetLibraryInfoRef)
REF_CLASS(LLVMTargetMachineRef, TargetMachineRef)
REF_CLASS(LLVMTargetRef, TargetRef)
REF_CLASS(LLVMTypeRef, TypeRef)
REF_CLASS(LLVMUseRef, UseRef)
REF_CLASS(LLVMValueRef, ValueRef)
REF_CLASS(LLVMRelocationIteratorRef, RelocationIteratorRef)
REF_CLASS(LLVMSectionIteratorRef, SectionIteratorRef)
REF_CLASS(LLVMSymbolIteratorRef, SymbolIteratorRef)
REF_CLASS(LLVMTargetOptionsRef, TargetOptionsRef)
REF_CLASS(LLVMAttributeRef, AttributeRef)
REF_CLASS(LLVMMetadataRef, MetadataRef)
REF_CLASS(LLVMModuleFlagEntry*, ModuleFlagEntry)


//
// Registering container classes that will be used to receive value by pointer
//
OUT_CLASS(LLVMMemoryBufferRef, MemoryBufferRefOut)
OUT_CLASS(LLVMModuleRef, ModuleRefOut)
OUT_CLASS(LLVMModuleProviderRef, ModuleProviderRefOut)
OUT_CLASS(LLVMTargetRef, TargetRefOut)
OUT_CLASS(jint, IntOut)
OUT_CLASS(size_t, SizeTOut)
OUT_CLASS(LLVMAttributeRef, AttributeRefOut)

// wrap char* -> charp otherwise macro fails
%{typedef char* charp;%}; typedef char* charp;
OUT_CLASS(charp, StringOut, if (self->value) free(self->value))

// wrap struct LongArray* -> LongArrayPtr here otherwise macro fails
%{typedef struct LongArray* LongArrayPtr;%}; typedef struct LongArray* LongArrayPtr;
OUT_CLASS(LongArrayPtr, LongArrayOut, if (self->value) free(self->value))


//
// Map pointer to container classes to receive value by pointer
//
OUT_ARG(MemoryBufferRefOut, LLVMMemoryBufferRef *OutMemBuf)
OUT_ARG(ModuleRefOut, LLVMModuleRef *OutM)
OUT_ARG(ModuleRefOut, LLVMModuleRef *OutModule)
OUT_ARG(ModuleProviderRefOut, LLVMModuleProviderRef *OutMP)
OUT_ARG(TargetRefOut, LLVMTargetRef *T)
OUT_ARG(StringOut, char **OutMessage)
OUT_ARG(StringOut, char **ErrorMessage)
OUT_ARG(IntOut, unsigned *Len)
OUT_ARG(IntOut, int* OutSize)
OUT_ARG(IntOut, LLVMBool *losesInfo)
OUT_ARG(SizeTOut, size_t* out)
OUT_ARG(LongArrayOut, uint64_t **Out)
OUT_ARG(AttributeRefOut, LLVMAttributeRef *Attrs)

//
// registering wrappers for arrays
//
ARRAY_CLASS(LLVMTypeRef, TypeRefArray)
ARRAY_CLASS(LLVMBasicBlockRef, BasicBlockRefArray)
ARRAY_CLASS(LLVMValueRef, ValueRefArray)
ARRAY_CLASS(jlong, LongArray)
ARRAY_CLASS(jint, IntArray)

ARRAY_ARG(TypeRefArray, LLVMTypeRef *)
ARRAY_ARG(BasicBlockRefArray, LLVMBasicBlockRef *)
ARRAY_ARG(ValueRefArray, LLVMValueRef *)
ARRAY_ARG(LongArray, jlong Words[])
ARRAY_ARG(IntArray, unsigned *IdxList)

//
// register special cases to be turned into arrays (see LLVMCommon)
//
%apply (char *ARRAY, size_t ARRAYSIZE) {(const char *InputData, size_t InputDataLength)};
%apply (char *ARRAY, size_t ARRAYSIZE) {(char *Dest, size_t DestSize)};

//
// register special cases to be turned into String (see LLVMCommon)
//
%apply (char *STRING, size_t STRINGSIZE) {(const char *Name, unsigned SLen)};
%apply (char *STRING, size_t STRINGSIZE) {(const char *Str, unsigned Length)};
%apply (char *STRING, size_t STRINGSIZE) {(const char *Str, unsigned SLen)};

//
// register out classes to be used for generic pointers
//
REF_PTR(size_t*, IntOut)
REF_PTR(uint64_t*, IntOut)
REF_PTR(int64_t*, IntOut)
REF_PTR(uint32_t*, IntOut)
REF_PTR(unsigned*, IntOut)
REF_PTR(LLVMBool*, IntOut)
REF_PTR(void*, IntOut)

//
// Other setups
//

// Prevent arguments named ContextRef to interfere with the type named ContextRef
#define ContextRef contextRef

// do not generate setter
%immutable llvmHostTriple;

// These return char* which the caller must free.
%newobject LLVMGetTargetMachineTriple;
%newobject LLVMGetTargetMachineCPU;
%newobject LLVMGetTargetMachineFeatureString;
%newobject LLVMCopyStringRepOfTargetData;
// release of these shall be done using LLVMDisposeMessage as per doc
%typemap(newfree) char * "LLVMDisposeMessage($1);";

// expand LLVMAttribute to int values(don't use it as java enum)
%typemap(javain) enum LLVMAttribute "$javainput"
%typemap(javaout) enum LLVMAttribute {
    return $jnicall;
  }
%typemap(jni) enum LLVMAttribute "jint"
%typemap(jtype) enum LLVMAttribute "int"
%typemap(jstype) enum LLVMAttribute "int"
%typemap(in) enum LLVMAttribute  %{ $1 = ($1_ltype)$input; %}
%typemap(out) enum LLVMAttribute  %{ $result = (jint)$1; %}
%typemap(directorout) enum LLVMAttribute  %{ $result = ($1_ltype)$input; %}
%typemap(directorin, descriptor="L$packagepath/$javaclassname;") enum LLVMAttribute "$input = (jint) $1;"
%typemap(javadirectorin) enum LLVMAttribute "$jniinput"
%typemap(javadirectorout) enum LLVMAttribute "$javacall"

// Wrap output stream using own wrapper implementation
%typemap(jtype) void *OutputStream "java.io.OutputStream"
%typemap(jstype) void *OutputStream "java.io.OutputStream"
%typemap(jni) void *OutputStream "jobject"
%typemap(javain) void *OutputStream "$javainput"
%typemap(in) void *OutputStream {
  if (!$input) {
    SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, NULL);
    return $null;
  }
  $1 = ($1_ltype) AllocOutputStreamWrapper(jenv, $input);
  if (!$1) return $null;
}
%typemap(freearg) void *OutputStream {
  FreeOutputStreamWrapper($1);
}


//
// Ignores
//

// Deprecated functions
%ignore LLVMGetBitcodeModuleProviderInContext;
%ignore LLVMGetBitcodeModuleProvider;
%ignore LLVMWriteBitcodeToFileHandle;

// Take String and size but String already has size so these don't make sense.
// There are versions that only take a String.
%ignore LLVMConstIntOfStringAndSize;
%ignore LLVMConstRealOfStringAndSize;

// This is inlined and based on macros and will only work as expected when
// called on the same platform as the llvm/Config/llvm-config.h file was
// generated for.
%ignore LLVMInitializeNativeTarget;

// ignore own helper methods
%ignore AllocOutputStreamWrapper;
%ignore FreeOutputStreamWrapper;

%ignore LLVMInstallFatalErrorHandler;
%ignore LLVMResetFatalErrorHandler;
%ignore LLVMContextSetDiagnosticHandler;
%ignore LLVMContextSetYieldCallback;

//
%ignore LLVMContextGetDiagnosticHandler;

// ignore command line and UIs
%ignore LLVMParseCommandLineOptions;

// ignore failed constants
%ignore INT64_MAX;
%ignore INT64_MIN;
%ignore UINT64_MAX;
%ignore LLVMAttributeReturnIndex;
%ignore LLVMAttributeFunctionIndex;

%include "llvm-c/Core.h"
%include "llvm-c/BitReader.h"
%include "llvm-c/BitWriter.h"
%include "llvm-c/Object.h"
%include "llvm-c/Transforms/IPO.h"
%include "llvm-c/Transforms/PassManagerBuilder.h"
%include "llvm-c/Transforms/Scalar.h"
%include "llvm-c/Transforms/Vectorize.h"
%include <llvm-c/Transforms/Utils.h>
%include "llvm-c/Target.h"
%include "llvm-c/TargetMachine.h"
%include "llvm-c/Linker.h"
%include "../native/LLVMExtra.h"
%include "../native/ClangExtra.h"

%pragma(java) jniclasscode=%{
  static {
    org.robovm.llvm.NativeLibrary.load();
  }
%}
