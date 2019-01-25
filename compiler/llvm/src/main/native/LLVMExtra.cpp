#include <llvm-c/Core.h>
#include <llvm-c/Object.h>
#include <llvm-c/TargetMachine.h>
#include <llvm-c/IRReader.h>
#include <llvm-c/Linker.h>
#include <llvm/DebugInfo/DWARF/DWARFContext.h>
#include <llvm/DebugInfo/DWARF/DWARFFormValue.h>
#include <llvm/IRReader/IRReader.h>
#include <llvm/IR/LLVMContext.h>
#include <llvm/IR/Module.h>
#include <llvm/IR/DataLayout.h>
#include <llvm/MC/MCAsmBackend.h>
#include <llvm/MC/MCAsmInfo.h>
#include <llvm/MC/MCContext.h>
#include <llvm/MC/MCInstPrinter.h>
#include <llvm/MC/MCInstrInfo.h>
#include <llvm/MC/MCObjectFileInfo.h>
#include <llvm/MC/MCParser/AsmLexer.h>
#include <llvm/MC/MCRegisterInfo.h>
#include <llvm/MC/MCSectionMachO.h>
#include <llvm/MC/MCStreamer.h>
#include <llvm/MC/MCSubtargetInfo.h>
#include <llvm/MC/MCParser/MCTargetAsmParser.h>
#include <llvm/MC/MCCodeEmitter.h>
#include <llvm/MC/MCObjectWriter.h>
#include <llvm/Object/ObjectFile.h>
#include <llvm/IR/LegacyPassManager.h>
#include <llvm/Transforms/IPO/PassManagerBuilder.h>
#include <llvm/Transforms/IPO/AlwaysInliner.h>
#include <llvm/Transforms/IPO.h>
#include <llvm/Target/TargetMachine.h>
#include <llvm/Target/TargetOptions.h>
#include <llvm/CodeGen/TargetSubtargetInfo.h>
#include <llvm/BinaryFormat/Dwarf.h>
#include <llvm/Support/FormattedStream.h>
#include <llvm/Support/TargetRegistry.h>
#include <llvm/Support/MemoryBuffer.h>
#include <llvm/Support/SourceMgr.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/Support/ToolOutputFile.h>
#include <cstring>
#include <string>
#include <stdio.h>
#include <locale.h>
#ifdef __APPLE__
#include <xlocale.h>
#endif

#include <jni.h>

#include "LLVMExtra.h"

using namespace llvm;
using namespace llvm::object;
using namespace dwarf;

DEFINE_SIMPLE_CONVERSION_FUNCTIONS(Target, LLVMTargetRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(TargetMachine, LLVMTargetMachineRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(TargetOptions, LLVMTargetOptionsRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(PassManagerBuilder, LLVMPassManagerBuilderRef)

inline OwningBinary<ObjectFile> *unwrap(LLVMObjectFileRef OF) {
    return reinterpret_cast<OwningBinary<ObjectFile> *>(OF);
}

// as it was removed in 3.9, check https://reviews.llvm.org/D19094
static LLVMContext &getGlobalContext() {
    static LLVMContext MyGlobalContext;
    return MyGlobalContext;
}

inline raw_ostream& dump_u32(raw_ostream &os, uint32_t u32) {
    // dump as little endian
    os << (uint8_t)(u32 & 0xFF);
    os << (uint8_t)((u32 >> 8) & 0xFF);
    os << (uint8_t)((u32 >> 16) & 0xFF);
    os << (uint8_t)((u32 >> 24) & 0xFF);
    return os;
}
const char *llvmHostTriple = LLVM_HOST_TRIPLE;

void LLVMPassManagerBuilderSetDisableTailCalls(LLVMPassManagerBuilderRef PMB,
                                               LLVMBool Value) {
    PassManagerBuilder *Builder = unwrap(PMB);
    Builder->DisableTailCalls = Value;
}

void LLVMPassManagerBuilderUseAlwaysInliner(LLVMPassManagerBuilderRef PMB, LLVMBool InsertLifetime) {
    PassManagerBuilder *Builder = unwrap(PMB);
    Builder->Inliner = createAlwaysInlinerLegacyPass(InsertLifetime);
}

LLVMBool LLVMParseIR(LLVMMemoryBufferRef MemBuf,
                     LLVMModuleRef *OutModule, char **OutMessage) {
    return LLVMParseIRInContext(wrap(&getGlobalContext()), MemBuf, OutModule, OutMessage);
}

LLVMTargetRef LLVMLookupTarget(const char *Triple, char **ErrorMessage) {
    std::string Error;
    const Target *TheTarget = TargetRegistry::lookupTarget(std::string(Triple), Error);
    if (!TheTarget) {
        *ErrorMessage = strdup(Error.c_str());
        return NULL;
    }
    return wrap(TheTarget);
}

LLVMBool LLVMTargetMachineGetAsmVerbosityDefault(LLVMTargetMachineRef T) {
    return unwrap(T)->Options.MCOptions.AsmVerbose;
}

void LLVMTargetMachineSetAsmVerbosityDefault(LLVMTargetMachineRef T, LLVMBool VerboseAsm) {
    unwrap(T)->Options.MCOptions.AsmVerbose = VerboseAsm;
}

LLVMBool LLVMTargetMachineGetDataSections(LLVMTargetMachineRef T) {
    return unwrap(T)->getDataSections();
}

LLVMBool LLVMTargetMachineGetFunctionSections(LLVMTargetMachineRef T) {
    return unwrap(T)->getFunctionSections();
}

void LLVMTargetMachineSetDataSections(LLVMTargetMachineRef T, LLVMBool Value) {
    unwrap(T)->Options.DataSections = (Value != 0);
}

void LLVMTargetMachineSetFunctionSections(LLVMTargetMachineRef T, LLVMBool Value) {
    unwrap(T)->Options.FunctionSections = Value != 0;
}

LLVMTargetOptionsRef LLVMGetTargetMachineTargetOptions(LLVMTargetMachineRef T) {
    TargetMachine *TM = unwrap(T);
    return wrap(&(TM->Options));
}

LLVMBool LLVMTargetOptionsGetPrintMachineCode(LLVMTargetOptionsRef O) { return (LLVMBool) unwrap(O)->PrintMachineCode; }
void LLVMTargetOptionsSetPrintMachineCode(LLVMTargetOptionsRef O, LLVMBool V) { unwrap(O)->PrintMachineCode = V; }

LLVMBool LLVMTargetOptionsGetUnsafeFPMath(LLVMTargetOptionsRef O) { return (LLVMBool) unwrap(O)->UnsafeFPMath; }
void LLVMTargetOptionsSetUnsafeFPMath(LLVMTargetOptionsRef O, LLVMBool V) { unwrap(O)->UnsafeFPMath = V; }

LLVMBool LLVMTargetOptionsGetNoInfsFPMath(LLVMTargetOptionsRef O) { return (LLVMBool) unwrap(O)->NoInfsFPMath; }
void LLVMTargetOptionsSetNoInfsFPMath(LLVMTargetOptionsRef O, LLVMBool V) { unwrap(O)->NoInfsFPMath = V; }

LLVMBool LLVMTargetOptionsGetNoNaNsFPMath(LLVMTargetOptionsRef O) { return (LLVMBool) unwrap(O)->NoNaNsFPMath; }
void LLVMTargetOptionsSetNoNaNsFPMath(LLVMTargetOptionsRef O, LLVMBool V) { unwrap(O)->NoNaNsFPMath = V; }

LLVMBool LLVMTargetOptionsGetHonorSignDependentRoundingFPMathOption(LLVMTargetOptionsRef O) { return (LLVMBool) unwrap(O)->HonorSignDependentRoundingFPMathOption; }
void LLVMTargetOptionsSetHonorSignDependentRoundingFPMathOption(LLVMTargetOptionsRef O, LLVMBool V) { unwrap(O)->HonorSignDependentRoundingFPMathOption = V; }

LLVMBool LLVMTargetOptionsGetNoZerosInBSS(LLVMTargetOptionsRef O) { return (LLVMBool) unwrap(O)->NoZerosInBSS; }
void LLVMTargetOptionsSetNoZerosInBSS(LLVMTargetOptionsRef O, LLVMBool V) { unwrap(O)->NoZerosInBSS = V; }

LLVMBool LLVMTargetOptionsGetGuaranteedTailCallOpt(LLVMTargetOptionsRef O) { return (LLVMBool) unwrap(O)->GuaranteedTailCallOpt; }
void LLVMTargetOptionsSetGuaranteedTailCallOpt(LLVMTargetOptionsRef O, LLVMBool V) { unwrap(O)->GuaranteedTailCallOpt = V; }

LLVMBool LLVMTargetOptionsGetEnableFastISel(LLVMTargetOptionsRef O) { return (LLVMBool) unwrap(O)->EnableFastISel; }
void LLVMTargetOptionsSetEnableFastISel(LLVMTargetOptionsRef O, LLVMBool V) { unwrap(O)->EnableFastISel = V; }

LLVMBool LLVMModuleGetPositionIndependentExecutable(LLVMModuleRef M) {
    return (LLVMBool) unwrap(M)->getPIELevel() > PIELevel::Default;
}

void LLVMModuleSetPositionIndependentExecutable(LLVMModuleRef M, LLVMBool V) {
    unwrap(M)->setPIELevel(V ? PIELevel::Large : PIELevel::Default);
}

LLVMBool LLVMTargetOptionsGetUseInitArray(LLVMTargetOptionsRef O) { return (LLVMBool) unwrap(O)->UseInitArray; }
void LLVMTargetOptionsSetUseInitArray(LLVMTargetOptionsRef O, LLVMBool V) { unwrap(O)->UseInitArray = V; }

LLVMFloatABIType LLVMTargetOptionsGetFloatABIType(LLVMTargetOptionsRef O) { return (LLVMFloatABIType) unwrap(O)->FloatABIType; }
void LLVMTargetOptionsSetFloatABIType(LLVMTargetOptionsRef O, LLVMFloatABIType V) { unwrap(O)->FloatABIType = (FloatABI::ABIType) V; }

LLVMFPOpFusionMode LLVMTargetOptionsGetAllowFPOpFusion(LLVMTargetOptionsRef O) { return (LLVMFPOpFusionMode) unwrap(O)->AllowFPOpFusion; }
void LLVMTargetOptionsSetAllowFPOpFusion(LLVMTargetOptionsRef O, LLVMFPOpFusionMode V) { unwrap(O)->AllowFPOpFusion = (FPOpFusion::FPOpFusionMode) V; }

static void assembleDiagHandler(const SMDiagnostic &Diag, void *Context) {
    raw_string_ostream *OS = (raw_string_ostream*) Context;
    Diag.print(0, *OS, false);
}

// helper to create byte[] from SmallVector
static jbyteArray vectorToByteArray(JNIEnv *jenv, SmallVector<char, 0> &OutVector) {
    jbyteArray data = jenv->NewByteArray((jsize) OutVector.size());
    if (jenv->ExceptionCheck())
        return NULL;
    
    jenv->SetByteArrayRegion(data, 0, (jsize) OutVector.size(), (const jbyte *) OutVector.data());
    if (jenv->ExceptionCheck())
        return NULL;
    
    return data;
}


static LLVMBool LLVMTargetMachineAssembleToOutputStream(LLVMTargetMachineRef TM, LLVMMemoryBufferRef Mem, raw_pwrite_stream &Out, LLVMBool RelaxAll, LLVMBool NoExecStack, char **ErrorMessage) {
    *ErrorMessage = NULL;
    
#if !defined(WIN32) && !defined(_WIN32)
    locale_t loc = newlocale(LC_ALL_MASK, "C", 0);
    locale_t oldLoc = uselocale(loc);
#endif
    
    TargetMachine *TheTargetMachine = unwrap(TM);
    const Target *TheTarget = &(TheTargetMachine->getTarget());
    
    std::string TripleName = TheTargetMachine->getTargetTriple().str();
    std::string MCPU = TheTargetMachine->getTargetCPU().str();
    std::string FeaturesStr = TheTargetMachine->getTargetFeatureString().str();
    Reloc::Model RelocModel = TheTargetMachine->getRelocationModel();
    CodeModel::Model CMModel = TheTargetMachine->getCodeModel();
    
    std::unique_ptr<MemoryBuffer> Buffer(unwrap(Mem));
    
    std::string DiagStr;
    raw_string_ostream DiagStream(DiagStr);
    SourceMgr SrcMgr;
    SrcMgr.setDiagHandler(assembleDiagHandler, &DiagStream);
    
    // Tell SrcMgr about this buffer, which is what the parser will pick up.
    SrcMgr.AddNewSourceBuffer(std::move(Buffer), SMLoc());
    
    // Record the location of the include directories so that the lexer can find
    // it later.
    //  SrcMgr.setIncludeDirs(IncludeDirs);
    
    std::unique_ptr<MCRegisterInfo> MRI(TheTarget->createMCRegInfo(TripleName));
    std::unique_ptr<MCAsmInfo> MAI(TheTarget->createMCAsmInfo(*MRI, TripleName));
    std::unique_ptr<MCObjectFileInfo> MOFI(new MCObjectFileInfo());
    MCContext Ctx(MAI.get(), MRI.get(), MOFI.get(), &SrcMgr);
    bool PIC = RelocModel == Reloc::Model::PIC_;
    MOFI->InitMCObjectFileInfo(Triple(TripleName), PIC, Ctx);
    
    std::unique_ptr<MCInstrInfo> MCII(TheTarget->createMCInstrInfo());
    std::unique_ptr<MCSubtargetInfo> STI(TheTarget->createMCSubtargetInfo(TripleName, MCPU, FeaturesStr));
    std::unique_ptr<MCStreamer> Str;
    std::unique_ptr<MCCodeEmitter> CE(TheTarget->createMCCodeEmitter(*MCII, *MRI, Ctx));
    MCTargetOptions MCOptions;
    std::unique_ptr<MCAsmBackend> MAB(TheTarget->createMCAsmBackend(*STI, *MRI, MCOptions));
    std::unique_ptr<MCObjectWriter> OW = MAB->createObjectWriter(Out);
    Str.reset(TheTarget->createMCObjectStreamer(Triple(TripleName), Ctx, std::move(MAB), std::move(OW),
                                                std::move(CE), *STI, RelaxAll != 0, /*IncrementalLinkerCompatible*/ false, /*DWARFMustBeAtTheEnd*/ true));
    if (NoExecStack != 0)
        Str->InitSections(true);
    
    std::unique_ptr<MCAsmParser> Parser(createMCAsmParser(SrcMgr, Ctx, *Str, *MAI));
    std::unique_ptr<MCTargetAsmParser> TAP(TheTarget->createMCAsmParser(*STI, *Parser, *MCII, MCOptions));
    if (!TAP) {
        *ErrorMessage = strdup("this target does not support assembly parsing");
        goto done;
    }
    
    Parser->setTargetParser(*TAP.get());
    
    if (Parser->Run(false)) {
        *ErrorMessage = strdup(DiagStream.str().c_str());
        goto done;
    }
    Out.flush();
    
done:
#if !defined(WIN32) && !defined(_WIN32)
    uselocale(oldLoc);
    freelocale(loc);
#endif
    return *ErrorMessage ? true : false;
}

// wrapper that returns byte array
jbyteArray LLVMTargetMachineAssemble(JNIEnv *jenv, LLVMTargetMachineRef TM, LLVMMemoryBufferRef Mem,
                                     LLVMBool RelaxAll, LLVMBool NoExecStack, char **ErrorMessage) {
    SmallVector<char, 0> OutVector;
    std::unique_ptr<raw_svector_ostream> BOS = make_unique<raw_svector_ostream>(OutVector);
    raw_pwrite_stream *Out = BOS.get();
    
    if (false == LLVMTargetMachineAssembleToOutputStream(TM, Mem, *Out, RelaxAll, NoExecStack, ErrorMessage)) {
        return vectorToByteArray(jenv, OutVector);
    }
    
    // failed
    return NULL;
}

static LLVMBool LLVMTargetMachineEmit(LLVMTargetMachineRef T, LLVMModuleRef M,
                                      raw_pwrite_stream &OS, LLVMCodeGenFileType codegen, char **ErrorMessage) {
    TargetMachine* TM = unwrap(T);
    Module* Mod = unwrap(M);
    legacy::PassManager CodeGenPasses;
    
    std::string error;
    
    TargetMachine::CodeGenFileType ft;
    switch (codegen) {
        case LLVMAssemblyFile:
            ft = TargetMachine::CGFT_AssemblyFile;
            break;
        default:
            ft = TargetMachine::CGFT_ObjectFile;
            break;
    }
    if (TM->addPassesToEmitFile(CodeGenPasses, OS, NULL, ft)) {
        error = "TargetMachine can't emit a file of this type";
        *ErrorMessage = strdup(error.c_str());
        return true;
    }
    
    CodeGenPasses.run(*Mod);
    
    OS.flush();
    return false;
}

static LLVMBool LLVMTargetMachineEmitToOutputStream(LLVMTargetMachineRef T, LLVMModuleRef M,
                                             raw_pwrite_stream &Out, LLVMCodeGenFileType codegen, char** ErrorMessage) {
    
#if !defined(WIN32) && !defined(_WIN32)
    locale_t loc = newlocale(LC_ALL_MASK, "C", 0);
    locale_t oldLoc = uselocale(loc);
#endif
    bool Result = LLVMTargetMachineEmit(T, M, Out, codegen, ErrorMessage);
    Out.flush();

#if !defined(WIN32) && !defined(_WIN32)
    uselocale(oldLoc);
    freelocale(loc);
#endif
    
    return Result;
}

// wrapper that returns byte array
jbyteArray LLVMTargetMachineEmit(JNIEnv *jenv, LLVMTargetMachineRef T, LLVMModuleRef M,
                                 LLVMCodeGenFileType codegen, char** ErrorMessage) {
    SmallVector<char, 0> OutVector;
    std::unique_ptr<raw_svector_ostream> BOS = make_unique<raw_svector_ostream>(OutVector);
    raw_pwrite_stream *Out = BOS.get();
    
    if (false == LLVMTargetMachineEmitToOutputStream(T, M, *Out, codegen, ErrorMessage)) {
        return vectorToByteArray(jenv, OutVector);
    }
    
    // failed
    return NULL;
}

void LLVMGetLineInfoForAddressRange(LLVMObjectFileRef O, uint64_t Address, uint64_t Size, size_t* OutSize, uint64_t** Out) {
    std::unique_ptr<DIContext> ctx = DWARFContext::create(*(unwrap(O)->getBinary()));
    DILineInfoTable lineTable = ctx->getLineInfoForAddressRange(Address, Size);
    *OutSize = lineTable.size();
    *Out = NULL;
    if (lineTable.size() > 0) {
        *Out = (uint64_t*) calloc(lineTable.size() * 2, sizeof(uint64_t));
        for (int i = 0; i < lineTable.size(); i++) {
            std::pair<uint64_t, DILineInfo> p = lineTable[i];
            (*Out)[i * 2] = p.first;
            (*Out)[i * 2 + 1] = p.second.Line;
        }
    }
}

size_t LLVMCopySectionContents(LLVMSectionIteratorRef SI, char* Dest, size_t DestSize) {
    uint64_t SectionSize = LLVMGetSectionSize(SI);
    size_t Size = SectionSize > DestSize ? DestSize : SectionSize;
    const char* Contents = LLVMGetSectionContents(SI);
    memcpy(Dest, Contents, (size_t) Size);
    return Size;
}


/**
 * recursive routine that handles everything inside routine unit
 * extracts variable name for now (can extract lexical blocks in future if required
 */
static void LLVMInternalDumpDwarfSubroutineDebugData(DWARFCompileUnit *cu,  DWARFDie &entry, raw_ostream &os) {
    if (entry.getTag() == DW_TAG_formal_parameter || entry.getTag() == DW_TAG_variable) {
        do {
            // get name and location
            const char *name = entry.getName(DINameKind::ShortName);
            if (!name)
                break;
            
            Optional<DWARFFormValue> value = entry.findRecursively(DW_AT_location);
            if (!value.hasValue())
                break;
            Optional<ArrayRef<uint8_t>> data = value.getValue().getAsBlock();
            if (!data.hasValue())
                break;
            
            size_t len = data.getValue().size();
            StringRef strRef((const char *)data.getValue().data(), len);
            DataExtractor extractor(strRef, cu->getContext().isLittleEndian(), 0);
            
            uint32_t offset = 0;
            uint8_t reg = extractor.getU8(&offset);
            int32_t reg_offset = (int32_t)extractor.getSLEB128(&offset);
            
            // flags -- currently only if it is parameter
            uint8_t flags = entry.getTag() == DW_TAG_formal_parameter  ? 1 : 0;
            
            // dump variable data
            dump_u32(os, (uint32_t)strlen(name));
            os << name;
            os << flags;
            os << reg;
            dump_u32(os, reg_offset);
        } while (false);
    }
    
    if (entry.hasChildren()) {
        auto child = entry.getFirstChild();
        LLVMInternalDumpDwarfSubroutineDebugData(cu, child, os);
    }
    
    entry = entry.getSibling();
    if (entry.isValid())
        LLVMInternalDumpDwarfSubroutineDebugData(cu, entry, os);
}

static void LLVMDumpDwarfDebugDataToOutputStream(LLVMObjectFileRef O, raw_pwrite_stream& os) {
    std::unique_ptr<DWARFContext> ctx = DWARFContext::create(*(unwrap(O)->getBinary()));
    int cuNum = ctx->getNumCompileUnits();
    
    for (int idx = 0; idx < cuNum; idx++) {
        DWARFCompileUnit *cu = ctx->getCompileUnitAtIndex(idx);
        
        DWARFDie entry = cu->getUnitDIE();
        if (entry.getTag() != DW_TAG_compile_unit)
            continue;
        if (!entry.hasChildren())
            continue;
        
        entry = entry.getFirstChild();
        do {
            // expect subprogram
            if (entry.isSubprogramDIE()) {
                // dump subprogram name
                const char *name = entry.getName(DINameKind::ShortName);
                if (name && strlen(name)) {
                    // starting subrotine block
                    dump_u32(os, (uint32_t)strlen(name));
                    os << name;
                    
                    DWARFDie routineEntry = entry.getFirstChild();
                    if (routineEntry.isValid())
                        LLVMInternalDumpDwarfSubroutineDebugData(cu, routineEntry, os);
                    
                    // end of subrotine zero marker
                    dump_u32(os, 0);
                }
            }
            
            // check next level element
            entry = entry.getSibling();
        } while (entry.isValid());
        
        // end of stream marker
        dump_u32(os, 0);
    }
    
    os.flush();
}

// wrapper that returns byte array
jbyteArray LLVMDumpDwarfDebugData(JNIEnv *jenv, LLVMObjectFileRef O) {
    SmallVector<char, 0> OutVector;
    std::unique_ptr<raw_svector_ostream> BOS = make_unique<raw_svector_ostream>(OutVector);
    raw_pwrite_stream *Out = BOS.get();
    
    LLVMDumpDwarfDebugDataToOutputStream(O, *Out);
    return vectorToByteArray(jenv, OutVector);
}


static void linkModulesDiagnosticHandler(LLVMDiagnosticInfoRef DI, void *C) {
    auto *Err = reinterpret_cast<std::string *>(C);
    char *CErr = LLVMGetDiagInfoDescription(DI);
    *Err = CErr;
    LLVMDisposeMessage(CErr);
}


// Emulates old LLVMLinkModules by using LLVMLinkModules2 + DiagnosticHandler to receive error message
LLVMBool LLVMLinkModules(LLVMModuleRef Dest, LLVMModuleRef Src, char **OutMessage) {
    // Symbol clash between two modules
    Module *m = unwrap(Dest);
    LLVMContext &Ctx = m->getContext();
    std::string Error;
    auto oldHandler = LLVMContextGetDiagnosticHandler(wrap(&Ctx));
    void* oldHandlerCtx = LLVMContextGetDiagnosticContext(wrap(&Ctx));
    LLVMContextSetDiagnosticHandler(wrap(&Ctx), linkModulesDiagnosticHandler, &Error);
    
    LLVMBool Result = LLVMLinkModules2(Dest, Src);
    *OutMessage = strdup(Error.c_str());
    
    // restore old handler
    LLVMContextSetDiagnosticHandler(wrap(&Ctx), oldHandler, oldHandlerCtx);

    return Result;
}
