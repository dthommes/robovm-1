#ifndef LLVM_EXTRA_H
#define LLVM_EXTRA_H

#include <llvm-c/Core.h>
#include <llvm-c/Transforms/PassManagerBuilder.h>
#include <llvm-c/Object.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum LLVMFloatABIType {
    LLVMFloatABITypeDefault,
    LLVMFloatABITypeSoft,
    LLVMFloatABITypeHard
} LLVMFloatABIType;

typedef enum LLVMFPOpFusionMode {
    LLVMFPOpFusionModeFast,
    LLVMFPOpFusionModeStandard,
    LLVMFPOpFusionModeStrict
} LLVMFPOpFusionMode;

typedef struct LLVMOpaqueTargetOptions *LLVMTargetOptionsRef;

extern const char *llvmHostTriple;

void LLVMPassManagerBuilderSetDisableTailCalls(LLVMPassManagerBuilderRef PMB,
                                            LLVMBool Value);
void LLVMPassManagerBuilderUseAlwaysInliner(LLVMPassManagerBuilderRef PMB, LLVMBool InsertLifetime);

LLVMBool LLVMParseIR(LLVMMemoryBufferRef MemBuf,
                          LLVMModuleRef *OutModule, char **OutMessage);

LLVMBool LLVMParseIRInContext(LLVMContextRef ContextRef,
                                   LLVMMemoryBufferRef MemBuf,
                                   LLVMModuleRef *OutModule,
                                   char **OutMessage);

LLVMTargetRef LLVMLookupTarget(const char *Triple, char **ErrorMessage);

LLVMBool LLVMTargetMachineGetAsmVerbosityDefault(LLVMTargetMachineRef T);
void LLVMTargetMachineSetAsmVerbosityDefault(LLVMTargetMachineRef T, LLVMBool VerboseAsm);
LLVMBool LLVMTargetMachineGetDataSections(LLVMTargetMachineRef T);
LLVMBool LLVMTargetMachineGetFunctionSections(LLVMTargetMachineRef T);
void LLVMTargetMachineSetDataSections(LLVMTargetMachineRef T, LLVMBool Value);
void LLVMTargetMachineSetFunctionSections(LLVMTargetMachineRef T, LLVMBool Value);

LLVMTargetOptionsRef LLVMGetTargetMachineTargetOptions(LLVMTargetMachineRef T);

LLVMBool LLVMTargetOptionsGetPrintMachineCode(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetPrintMachineCode(LLVMTargetOptionsRef O, LLVMBool V);
LLVMBool LLVMTargetOptionsGetUnsafeFPMath(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetUnsafeFPMath(LLVMTargetOptionsRef O, LLVMBool V);
LLVMBool LLVMTargetOptionsGetNoInfsFPMath(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetNoInfsFPMath(LLVMTargetOptionsRef O, LLVMBool V);
LLVMBool LLVMTargetOptionsGetNoNaNsFPMath(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetNoNaNsFPMath(LLVMTargetOptionsRef O, LLVMBool V);
LLVMBool LLVMTargetOptionsGetHonorSignDependentRoundingFPMathOption(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetHonorSignDependentRoundingFPMathOption(LLVMTargetOptionsRef O, LLVMBool V);
LLVMBool LLVMTargetOptionsGetNoZerosInBSS(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetNoZerosInBSS(LLVMTargetOptionsRef O, LLVMBool V);
LLVMBool LLVMTargetOptionsGetGuaranteedTailCallOpt(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetGuaranteedTailCallOpt(LLVMTargetOptionsRef O, LLVMBool V);
LLVMBool LLVMTargetOptionsGetEnableFastISel(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetEnableFastISel(LLVMTargetOptionsRef O, LLVMBool V);
LLVMBool LLVMModuleGetPositionIndependentExecutable(LLVMModuleRef M);
void LLVMModuleSetPositionIndependentExecutable(LLVMModuleRef M, LLVMBool V);
LLVMBool LLVMTargetOptionsGetUseInitArray(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetUseInitArray(LLVMTargetOptionsRef O, LLVMBool V);
LLVMFloatABIType LLVMTargetOptionsGetFloatABIType(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetFloatABIType(LLVMTargetOptionsRef O, LLVMFloatABIType V);
LLVMFPOpFusionMode LLVMTargetOptionsGetAllowFPOpFusion(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetAllowFPOpFusion(LLVMTargetOptionsRef O, LLVMFPOpFusionMode V);
LLVMBool LLVMLinkModules(LLVMModuleRef Dest, LLVMModuleRef Src, char **OutMessage);

jbyteArray LLVMTargetMachineAssemble(JNIEnv *jenv, LLVMTargetMachineRef TM, LLVMMemoryBufferRef Mem,
    LLVMBool RelaxAll, LLVMBool NoExecStack, char **ErrorMessage);
jbyteArray LLVMTargetMachineEmit(JNIEnv *jenv, LLVMTargetMachineRef T, LLVMModuleRef M,
    LLVMCodeGenFileType codegen, char** ErrorMessage);

void LLVMGetLineInfoForAddressRange(LLVMObjectFileRef O, uint64_t Address, uint64_t Size, size_t* OutSize, uint64_t** Out);
size_t LLVMCopySectionContents(LLVMSectionIteratorRef SI, char* Dest, size_t DestSize);


// dumps DWARF debug information into output stream
jbyteArray LLVMDumpDwarfDebugData(JNIEnv *jenv, LLVMObjectFileRef O);

// type checkers of obj file
LLVMBool LLVMIsMachOObjectFile(LLVMObjectFileRef objectFile);
LLVMBool LLVMIsCOFFObjectFile(LLVMObjectFileRef objectFile);


// need symbol flags for debug
typedef  enum LLVMSymbolFlags {
    SF_None = 0,
    SF_Undefined = 1 << 0,      // Symbol is defined in another object file
    SF_Global = 1 << 1,         // Global symbol
    SF_Weak = 1 << 2,           // Weak symbol
    SF_Absolute = 1 << 3,       // Absolute symbol
    SF_Common = 1 << 4,         // Symbol has common linkage
    SF_Indirect = 1 << 5,       // Symbol is an alias to another symbol
    SF_Exported = 1 << 6,       // Symbol is visible to other DSOs
    SF_FormatSpecific = 1 << 7, // Specific to the object file format
                                 // (e.g. section symbols)
    SF_Thumb = 1 << 8,          // Thumb symbol in a 32-bit ARM binary
    SF_Hidden = 1 << 9,         // Symbol has hidden visibility
    SF_Const = 1 << 10,         // Symbol value is constant
    SF_Executable = 1 << 11,    // Symbol points to an executable section                                // (IR only)
} LLVMSymbolFlags;

uint32_t LLVMGetSymbolFlags(LLVMSymbolIteratorRef SI);

#ifdef __cplusplus
}
#endif

#endif
