#include <clang/CodeGen/CodeGenAction.h>
#include <clang/Frontend/CompilerInstance.h>
#include <clang/Frontend/CompilerInvocation.h>
#include <clang/Basic/DiagnosticOptions.h>
#include <clang/Frontend/TextDiagnosticPrinter.h>
#include <clang/Frontend/FrontendDiagnostic.h>
#include <clang/Lex/PreprocessorOptions.h>
#include <llvm/ADT/IntrusiveRefCntPtr.h>
#include <llvm/IR/Module.h>
#include <llvm-c/Core.h>

#include "ClangExtra.h"

using namespace std;
using namespace llvm;
using namespace clang;

LLVMModuleRef ClangCompileFile(LLVMContextRef Context, char* Data, char* FileName, char* Triple, char **ErrorMessage)
{
    std::string error;
    raw_string_ostream error_os(error);

    std::unique_ptr<CompilerInstance> Clang(new CompilerInstance());
    IntrusiveRefCntPtr<DiagnosticIDs> DiagID(new DiagnosticIDs());

    // The compiler invocation needs a DiagnosticsEngine so it can report problems
    IntrusiveRefCntPtr<DiagnosticOptions> DiagOpts = new DiagnosticOptions();
    DiagOpts->ShowCarets = false;
    TextDiagnosticPrinter *DiagClient = new TextDiagnosticPrinter(error_os, &*DiagOpts);
    DiagnosticsEngine Diags(DiagID, &*DiagOpts, DiagClient);
    if (!CompilerInvocation::CreateFromArgs(Clang->getInvocation(), 0, 0, Diags)) {
        *ErrorMessage = strdup("Error creating compiler invocation!");
        return NULL;
    }

    // Create the compiler invocation
    Clang->getInvocation().getPreprocessorOpts().addRemappedFile(FileName, MemoryBuffer::getMemBuffer(Data).release());
    Clang->getInvocation().getFrontendOpts().Inputs.clear();
    Clang->getInvocation().getFrontendOpts().Inputs.push_back(FrontendInputFile(FileName, InputKind::C));
    if (Triple) {
        Clang->getInvocation().getTargetOpts().Triple = Triple;
    }

    // Create the actual diagnostics engine.
    Clang->createDiagnostics();
    if (!Clang->hasDiagnostics()) {
        *ErrorMessage = strdup("Error creating Diagnostics!");
        return NULL;
    }

    // Create and execute the frontend to generate an LLVM bitcode module.
    std::unique_ptr<CodeGenAction> Act(new EmitLLVMOnlyAction(unwrap(Context)));
    if (!Clang->ExecuteAction(*Act) || Clang->getDiagnostics().hasErrorOccurred()) {
        *ErrorMessage = strdup(error.c_str());
        return NULL;
    }

    *ErrorMessage = strdup(error.c_str());

    return wrap((*Act).takeModule().release());
}
