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
LLVMBool LLVMTargetOptionsGetPositionIndependentExecutable(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetPositionIndependentExecutable(LLVMTargetOptionsRef O, LLVMBool V);
LLVMBool LLVMTargetOptionsGetUseInitArray(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetUseInitArray(LLVMTargetOptionsRef O, LLVMBool V);
LLVMFloatABIType LLVMTargetOptionsGetFloatABIType(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetFloatABIType(LLVMTargetOptionsRef O, LLVMFloatABIType V);
LLVMFPOpFusionMode LLVMTargetOptionsGetAllowFPOpFusion(LLVMTargetOptionsRef O);
void LLVMTargetOptionsSetAllowFPOpFusion(LLVMTargetOptionsRef O, LLVMFPOpFusionMode V);

    int LLVMTargetMachineAssembleToOutputStream(LLVMTargetMachineRef TM, LLVMMemoryBufferRef Mem, llvm::raw_pwrite_stream &Out,
    LLVMBool RelaxAll, LLVMBool NoExecStack, char **ErrorMessage);
LLVMBool LLVMTargetMachineEmitToOutputStream(LLVMTargetMachineRef T, LLVMModuleRef M,
    llvm::raw_pwrite_stream &Out, LLVMCodeGenFileType codegen, char** ErrorMessage);

void LLVMGetLineInfoForAddressRange(LLVMObjectFileRef O, uint64_t Address, uint64_t Size, int* OutSize, uint64_t** Out);
size_t LLVMCopySectionContents(LLVMSectionIteratorRef SI, char* Dest, size_t DestSize);


// dumps DWARF debug information into output stream
void LLVMDumpDwarfDebugDataToOutputStream(LLVMObjectFileRef O, llvm::raw_pwrite_stream& os);

#ifdef __cplusplus
}
#endif

#endif
