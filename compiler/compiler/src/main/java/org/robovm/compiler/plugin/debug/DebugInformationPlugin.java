/*
 * Copyright 2016 Justin Shapcott.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.robovm.compiler.plugin.debug;

import org.robovm.compiler.Annotations;
import org.robovm.compiler.Functions;
import org.robovm.compiler.ModuleBuilder;
import org.robovm.compiler.Symbols;
import org.robovm.compiler.Version;
import org.robovm.compiler.clazz.Clazz;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.llvm.Alloca;
import org.robovm.compiler.llvm.ArrayType;
import org.robovm.compiler.llvm.BasicBlock;
import org.robovm.compiler.llvm.Call;
import org.robovm.compiler.llvm.Constant;
import org.robovm.compiler.llvm.ConstantBitcast;
import org.robovm.compiler.llvm.Function;
import org.robovm.compiler.llvm.FunctionDeclaration;
import org.robovm.compiler.llvm.Global;
import org.robovm.compiler.llvm.Instruction;
import org.robovm.compiler.llvm.IntegerConstant;
import org.robovm.compiler.llvm.Linkage;
import org.robovm.compiler.llvm.MetadataValue;
import org.robovm.compiler.llvm.debug.dwarf.DIMetadataValueList;
import org.robovm.compiler.llvm.NamedMetadata;
import org.robovm.compiler.llvm.PointerType;
import org.robovm.compiler.llvm.Type;
import org.robovm.compiler.llvm.Value;
import org.robovm.compiler.llvm.Variable;
import org.robovm.compiler.llvm.VariableRef;
import org.robovm.compiler.llvm.ZeroInitializer;
import org.robovm.compiler.llvm.debug.dwarf.DIBasicType;
import org.robovm.compiler.llvm.debug.dwarf.DICompileUnit;
import org.robovm.compiler.llvm.debug.dwarf.DIFile;
import org.robovm.compiler.llvm.debug.dwarf.DILocation;
import org.robovm.compiler.llvm.debug.dwarf.DILocalVariable;
import org.robovm.compiler.llvm.debug.dwarf.DILocalVariableList;
import org.robovm.compiler.llvm.debug.dwarf.DISubprogram;
import org.robovm.compiler.llvm.debug.dwarf.DISubroutineType;
import org.robovm.compiler.llvm.debug.dwarf.DwarfConst;
import org.robovm.compiler.plugin.AbstractCompilerPlugin;
import org.robovm.compiler.plugin.PluginArgument;
import org.robovm.compiler.plugin.PluginArguments;
import org.robovm.llvm.ObjectFile;
import org.robovm.llvm.debuginfo.DebugMethodInfo;
import org.robovm.llvm.debuginfo.DebugObjectFileInfo;
import org.robovm.llvm.debuginfo.DebugVariableInfo;
import soot.Local;
import soot.LocalVariable;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IdentityStmt;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceFileTag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * provides only line number debug information for now
 */
public class DebugInformationPlugin extends AbstractCompilerPlugin {


    public DebugInformationPlugin() {
    }

    public PluginArguments getArguments() {
        return new PluginArguments("debug", Collections.<PluginArgument>emptyList());
    }

    @Override
    public void helloClass(Config config, Clazz clazz) {
        super.helloClass(config, clazz);

        ClassDataBundle classBundle = clazz.getAttachment(ClassDataBundle.class);
        if (classBundle != null)
            clazz.removeAttachement(classBundle);

        // keep all data for class in one structure, allows to reset thing by placing null there
        classBundle = new ClassDataBundle();
        clazz.attach(classBundle);

        if (config.isDebug()) {
            // make a list of java methods as it is in class
            // as during compilation class going to be heavily adjusted and lot of synthetics method will appear
            classBundle.methodsBeforeCompile = new HashSet<>();
            for (SootMethod m : clazz.getSootClass().getMethods()) {
                if (m.isAbstract() || m.isNative())
                    continue;
                classBundle.methodsBeforeCompile.add(m.getSignature());
            }
        }
    }

    @Override
    public void beforeClass(Config config, Clazz clazz, ModuleBuilder mb) throws IOException {
        super.beforeClass(config, clazz, mb);

        String producer = "RoboVM " + Version.getVersion();
        ClassDataBundle classBundle = clazz.getAttachment(ClassDataBundle.class);
        // diFile and diCompileUnit
        classBundle.diFile = new DIFile(mb, getDwarfSourceFile(clazz), getDwarfSourceFilePath(clazz));
        classBundle.diCompileUnit =  new DICompileUnit(mb, classBundle.diFile, !config.isDebug(), producer,
                        config.isDebug() ? DICompileUnit.DebugEmissionKind.FullDebug : DICompileUnit.DebugEmissionKind.LineTablesOnly);

        //noinspection unused
        NamedMetadata dwarfCompileUnit = NamedMetadata.withNamedTuple(mb, "llvm.dbg.cu", classBundle.diCompileUnit);

        // module flags
        NamedMetadata dwarfVersion = NamedMetadata.withTuple(mb, DwarfConst.ModuleFlagBehavior.Warning.raw,
                "Dwarf Version", DwarfConst.LLVMConstants.DWARF_VERSION);
        NamedMetadata debugInfoVersion = NamedMetadata.withTuple(mb,DwarfConst.ModuleFlagBehavior.Warning.raw,
                "Debug Info Version", DwarfConst.LLVMConstants.DEBUG_INFO_VERSION);
        classBundle.flags = NamedMetadata.withNamedTuple(mb, "llvm.module.flags", dwarfVersion, debugInfoVersion);
        // llvm.ident
        //noinspection unused
        NamedMetadata ident = NamedMetadata.withNamedTuple(mb, "lllvm.ident", NamedMetadata.withTuple(mb, producer));

        if (config.isDebug()) {
            // create a list where method inforation will be saved
            classBundle.methods = new ArrayList<>();

            // register llvm.dbg.declare
            mb.addFunctionDeclaration(new FunctionDeclaration(Functions.LLVM_DBG_DECLARE));

            if (config.getTarget().getArch().isArm()) {
                // add global variable to emit sp-fp offset
                // refer to 04-emit-sp-fp-offset-on-arm for details
                mb.addGlobal(new Global("robovm.emitSpFpOffsets", Type.I8));
            }
        }
    }

    @Override
    public void afterMethod(Config config, Clazz clazz, SootMethod method, ModuleBuilder mb, Function function) throws IOException {
        super.afterMethod(config, clazz, method, mb, function);

        ClassDataBundle classBundle = clazz.getAttachment(ClassDataBundle.class);

        // don't try to generate shadow frames for native or abstract methods
        // or methods that don't have any instructions in them
        if (method.isNative() || method.isAbstract() || !method.hasActiveBody()) {
            return;
        }

        BasicBlock entryBlock = function.getBasicBlocks().get(0);

        //Method has only a return null statement
        if (entryBlock.getInstructions().size() == 1) {
            return;
        }

        // build unit to line map
        Map<Unit, Integer> unitToLine = new HashMap<>();
        for (Unit unit : method.getActiveBody().getUnits()) {
            LineNumberTag tag = (LineNumberTag) unit.getTag("LineNumberTag");
            if (tag != null)
                unitToLine.put(unit, tag.getLineNumber());

        }

        // find out if debug information to be fetched for this method
        // skip debug information for class initializer, generated callbacks and bridge methods
        boolean includeDebuggerInfo = config.isDebug()
                && !Annotations.hasCallbackAnnotation(method)
                && !Annotations.hasBridgeAnnotation(method)
                && classBundle.methodsBeforeCompile.contains(method.getSignature());

        // maps for debugger
        Map<Unit, Instruction> unitToInstruction = new HashMap<>();
        Map<Integer, Alloca> varIndexToAlloca = new HashMap<>();
        Map<Integer, Integer> varIndexToArgNo = new HashMap<>();
        Map<Instruction, Integer> instructionToLineNo = new HashMap<>();

        // find line numbers
        int methodLineNumber = Integer.MAX_VALUE;
        int methodEndLineNumber = Integer.MIN_VALUE;
        for (BasicBlock bb : function.getBasicBlocks()) {
            for (Instruction instruction : bb.getInstructions()) {
                List<Object> units = instruction.getAttachments();
                Integer lineNumObj = null;
                for (Object object : units) {
                    if (!(object instanceof Unit))
                        continue;
                    Unit unit = (Unit) object;

                    // attach line number metadata only once
                    if (lineNumObj == null) {
                        lineNumObj = unitToLine.get(object);
                        if (lineNumObj != null) {
                            int currentLineNumber = lineNumObj;
                            methodLineNumber = Math.min(methodLineNumber, currentLineNumber);
                            methodEndLineNumber = Math.max(methodEndLineNumber, currentLineNumber);
                            instructionToLineNo.put(instruction, lineNumObj);
                        }
                    }

                    // build map for debugger
                    if (includeDebuggerInfo) {
                        if (!unitToInstruction.containsKey(unit))
                            unitToInstruction.put(unit, instruction);
                    }
                }

                if (includeDebuggerInfo) {
                    // if it is alloca -- save it to map (
                    if (instruction instanceof Alloca) {
                        Alloca alloca = (Alloca) instruction;
                        // find Local
                        for (Object o : alloca.getAttachments()) {
                            if (!(o instanceof Local))
                                continue;
                            Local local = (Local) o;
                            if (local.getVariableTableIndex() < 0)
                                continue;

                            Set<Integer> varIndexes = local.getSameSlotVariables();
                            if (varIndexes == null)
                                varIndexes = Collections.singleton(local.getVariableTableIndex());
                            for (Integer varIdx : varIndexes) {
                                if (varIndexToAlloca.containsKey(varIdx)) {
                                    // if there is more than one index used for local then we can't
                                    // connect it with local variable and lets invalidate this index
                                    // with null value. corresponding Locals should be picked from ValueBox
                                    varIndexToAlloca.put(varIdx, null);
                                } else {
                                    varIndexToAlloca.put(varIdx, alloca);
                                }
                            }
                        }
                    }
                } // if (includeDebuggerInfo)
            }
        }

        if (methodLineNumber == Integer.MAX_VALUE) {
            // there was no debug information for this method
            // it will be not possible to resolve variables, just return
            return;
        }

        // forward definition for variables
        // FIXME: seems locals list is not supported in DISubprogram, don't add it for now
        // DILocalVariableList diVariableList = new DILocalVariableList(mb);

        // forward definition for subprogram
        DISubprogram diSubprogram = new DISubprogram(mb, function.ref().getName(), classBundle.diFile,
                methodLineNumber, classBundle.getDummySubprogramType(mb), classBundle.diCompileUnit,
                null /*diVariableList*/);

        // add debug information to function attributes
        function.setDebugMetadata(diSubprogram.toDebugMetadata());

        // use cache to re-use same DILocation for same line/col
        LocationCache diLocationCache = new LocationCache(mb, diSubprogram);

        // attach debug line number information to each instruction that is in map
        for (Map.Entry<Instruction, Integer> e : instructionToLineNo.entrySet()) {
            Instruction instruction = e.getKey();
            int lineNo = e.getValue();
            instruction.addMetadata(diLocationCache.get(lineNo, 0).toDebugMetadata());
        }


        if (!includeDebuggerInfo) {
            return;
        }

        //
        //
        // debugger only part bellow
        //
        //

        // make a list of instructions for instrumented hook call
        // need to skip identity instructions, otherwise there is a risk that debugger will stop before arguments are
        // being copied to locals
        Unit firstHooksUnit = null;
        for (Unit unit : method.getActiveBody().getUnits()) {
            if (!(unit instanceof IdentityStmt)) {
                firstHooksUnit = unit;
                break;
            }
        }

        Instruction firstHooksInst = firstHooksUnit != null ? unitToInstruction.get(firstHooksUnit) : null;
        if (firstHooksInst != null) {
            // get all instruction that are subject to be instrumented after last Identity Statement
            Map<Integer, Instruction> hookInstructionLines = new HashMap<>();
            for (BasicBlock bb : function.getBasicBlocks()) {
                for (Instruction instruction : bb.getInstructions()) {
                    if (firstHooksInst != null && firstHooksInst != instruction)
                        continue;
                    firstHooksInst = null;
                    Integer lineNo = instructionToLineNo.get(instruction);
                    if (lineNo == null || hookInstructionLines.containsKey(lineNo))
                        continue;
                    hookInstructionLines.put(lineNo, instruction);
                }
            }

            // instrument hooks call, there is known line range, create global for method breakpoints
            int arraySize = ((methodEndLineNumber - methodLineNumber + 1) + 7) / 8;
            // global value to this array (without values as zeroinit)
            Global bpTable = new Global(Symbols.bptableSymbol(method), Linkage.internal,
                    new ZeroInitializer(new ArrayType(arraySize, Type.I8)));
            mb.addGlobal(bpTable);
            // cast to byte pointer
            ConstantBitcast bpTableRef = new ConstantBitcast(bpTable.ref(), Type.I8_PTR);
            for (Map.Entry<Integer, Instruction> e : hookInstructionLines.entrySet()) {
                int lineNo = e.getKey();
                injectHookInstrumented(diLocationCache, lineNo, lineNo - methodLineNumber, function, bpTableRef, e.getValue());
            }
        }

        // build map of local index to argument no
        int firstArgNo;
        if (!method.isStatic()) {
            // add map of 'this', as first argument, it always goes at slot=0
            varIndexToArgNo.put(0, 2);
            // skip it in arg map
            firstArgNo = 3;
        } else {
            firstArgNo = 2;
        }
        for (int idx = 0; idx < method.getParameterCount(); idx++) {
            int localIdx = method.getActiveBody().getParameterLocal(idx).getVariableTableIndex();
            varIndexToArgNo.put(localIdx, firstArgNo + idx);
        }

        // build local variable list
        int variableIdx = 1;
        List<VariableDataBundle> variables = new ArrayList<>();

        // insert variables
        for (LocalVariable var : method.getActiveBody().getLocalVariables()) {
            // skip local variables that attached to zero length code sequences
            if (var.getStartUnit() == null)
                continue;

            // find corresponding local alloca
            Alloca alloca = varIndexToAlloca.get(var.getIndex());
            if (alloca == null) {
                config.getLogger().error("Unable to resolve variable %s in method %s, class %s", var.getName(),
                        method.getName(), clazz.getClassName());
                continue;
            }

            // get line number information
            Integer lineObj = unitToLine.get(var.getStartUnit());
            int varStartLine = lineObj != null ? lineObj : methodLineNumber;
            int varEndLine;
            if (var.getEndUnit() != null) {
                lineObj = unitToLine.get(var.getEndUnit());
                varEndLine = lineObj != null ? lineObj : methodLineNumber;
            } else {
                varEndLine = methodEndLineNumber;
            }
            if (varStartLine > varEndLine)
                varEndLine = varStartLine;

            // variable is known, remember it
            // use special dwarf name just to be able make difference of variables with same name (e.g. there could be
            // multiple "i" variables
            variableIdx += 1;
            String dwarfName = "dw" + variableIdx + "_" + var.getName();
            VariableDataBundle variableBundle = new VariableDataBundle(var.getIndex(), var.getName(), dwarfName, var.getDescriptor(),
                    varStartLine, varEndLine);
            variables.add(variableBundle);

            // get arg idx if it is there
            Integer argIdxObj = varIndexToArgNo.get(var.getIndex());
            // add llvm.dbg.declare call
            DILocalVariable diLocalVariable;
            if (argIdxObj != null) {
                // argument
                diLocalVariable = new DILocalVariable(mb, dwarfName, varStartLine, argIdxObj,
                        classBundle.diFile, diSubprogram, classBundle.getDummyJavaVariableType(mb));
            } else {
                // local
                diLocalVariable = new DILocalVariable(mb, dwarfName, varStartLine, classBundle.diFile,
                        diSubprogram, classBundle.getDummyJavaVariableType(mb));
            }

            // get instruction to work with
            Instruction instr = unitToInstruction.get(var.getStartUnit());

            // right after alloca
            Call call = new Call(Functions.LLVM_DBG_DECLARE,
                    new MetadataValue(new VariableRef(alloca.getResult().getName(), new PointerType(alloca.getResult().getType()))),
                    diLocalVariable.ref(), new MetadataValue(new NamedMetadata.Ref("DIExpression()")));
            call.addMetadata((diLocationCache.get(varStartLine, 0)).toDebugMetadata());
            instr.getBasicBlock().insertBefore(instr, call);

            // save variable to the list
            // FIXME: it is not required for now, commenting out for now
            // diVariableList.add(diLocalVariable);
        }

        // sort variables by index to make sure arguments pop to top
        Collections.sort(variables, new Comparator<VariableDataBundle>() {
            @Override
            public int compare(VariableDataBundle o1, VariableDataBundle o2) {
                return o1.codeIndex - o2.codeIndex;
            }
        });

        // remember method debug information
        classBundle.methods.add(new MethodDataBundle(function.getName(), methodLineNumber, methodEndLineNumber, variables));
    }

    @Override
    public void afterObjectFile(Config config, Clazz clazz, File objectFile, ObjectFile objectFileData) throws IOException {
        super.afterObjectFile(config, clazz, objectFile, objectFileData);

        ClassDataBundle classBundle = clazz.getAttachment(ClassDataBundle.class);

        // pick DWARF debug information about local variables from objective file
        if (config.isDebug()) {

            // get debug information from objective file and write it to file cache
            DebugObjectFileInfo debugInfo = objectFileData.getDebugInfo();
            if (debugInfo != null) {
                // now it is a task to combine it with data received during compilation
                List<DebugMethodInfo> methods = new ArrayList<>();
                for (MethodDataBundle methodBundle : classBundle.methods) {
                    DebugMethodInfo dbgMethodInfo = debugInfo.methodBySignature(methodBundle.signature);
                    if (dbgMethodInfo == null) {
                        config.getLogger().warn("Failed to get debug information for method %s in class %s",
                                methodBundle.signature, clazz.getClassName());
                        continue;
                    }

                    // get variables
                    List<DebugVariableInfo> variables = new ArrayList<>();
                    for (VariableDataBundle variableBudnle : methodBundle.variables) {
                        DebugVariableInfo dbgVariableInfo = dbgMethodInfo.variableByName(variableBudnle.dwarfName);
                        if (dbgVariableInfo == null) {
                            config.getLogger().warn("Failed to get debug information for variable %s in method %s in class %s",
                                    variableBudnle.name, methodBundle.signature, clazz.getClassName());
                            continue;
                        }

                        // save variable
                        variables.add(new DebugVariableInfo(variableBudnle.name, variableBudnle.typeSignature, dbgVariableInfo.isArgument(),
                                variableBudnle.startLine, variableBudnle.finalLine, dbgVariableInfo.register(), dbgVariableInfo.offset()));
                    }

                    // remove class prefix from method name
                    String methodName = dbgMethodInfo.signature();
                    if (methodName.startsWith("[J]" + clazz.getClassName() + "."))
                        methodName = methodName.substring(clazz.getClassName().length() + 4);
                    // save method
                    methods.add(new DebugMethodInfo(methodName, variables.toArray(new DebugVariableInfo[0]),
                            methodBundle.startLine, methodBundle.finalLine));
                }

                // dump final info to file
                DebugObjectFileInfo finalDebugInfo = clazz.getAttachment(DebugObjectFileInfo.class);
                if (finalDebugInfo != null)
                    clazz.removeAttachement(finalDebugInfo);
                finalDebugInfo = new DebugObjectFileInfo(getJdwpSourceFile(clazz), methods.toArray(new DebugMethodInfo[0]));

                // save as attachment to class file
                clazz.attach(finalDebugInfo);
            }
        }

        // reset bundle data
        clazz.removeAttachement(classBundle);
    }

    /**
     * injects calls to _bcHookInstrumented to allow breakpoints/step by step debugging
     */
    private void injectHookInstrumented(LocationCache locationCache, int lineNo, int lineNumberOffset, Function function, Constant bpTableRef, Instruction instruction) {
        BasicBlock block = instruction.getBasicBlock();
        // prepare a call to following function:
        // void _bcHookInstrumented(DebugEnv* debugEnv, jint lineNumber, jint lineNumberOffset, jbyte* bptable, void* pc)

        // pick params
        Value debugEnv = function.getParameterRef(0);
        Variable pc = function.newVariable(Type.I8_PTR);
        Call getPcCall = new Call(pc, Functions.GETPC, new Value[0]);
        block.insertBefore(instruction, getPcCall);

        // lineNumberOffset is zero as single breakpoint table per class
        Call bcHookInstrumented = new Call(Functions.BC_HOOK_INSTRUMENTED, debugEnv, new IntegerConstant(lineNo),
                new IntegerConstant(lineNumberOffset), bpTableRef, pc.ref());
        block.insertBefore(instruction, bcHookInstrumented);

        // attach line number metadata otherwise stack entry will have previous line number index
        bcHookInstrumented.addMetadata(locationCache.get(lineNo, 0).toDebugMetadata());
    }

    /**
     * Simple file name resolution to be included as Dwarf debug entry, for LineNumbers there is no need in absolute file location, just in name
     */
    private String getDwarfSourceFile(Clazz clazz) {
        String sourceFile;
        String ext = ".java";
        String className = clazz.getInternalName();
        // create source file name from class internal name to preserve full path to inner classes and
        // lambdas as it is required for dsymutils fix
        // but also look for "SourceFileTag" to pick up proper extension in case of kotlin and others
        SourceFileTag sourceFileTag = (SourceFileTag) clazz.getSootClass().getTag("SourceFileTag");
        if (sourceFileTag != null) {
            String tagSourceFile = sourceFileTag.getSourceFile();
            int extIdx = tagSourceFile.lastIndexOf('.');
            if (extIdx > 0)
                ext = tagSourceFile.substring(extIdx);
        }

        if (className.contains("/"))
            sourceFile = className.substring(clazz.getInternalName().lastIndexOf("/") + 1) + ext;
        else
            sourceFile = className + ext;

        return sourceFile;
    }

    /**
     * Simple source file path resolution
     */
    private String getDwarfSourceFilePath(Clazz clazz) {
        String sourcePath = clazz.getPath().toString();
        if (!sourcePath.endsWith("/"))
            sourcePath += "/";
        if (!sourcePath.startsWith("/"))
            sourcePath = "/" + sourcePath;

        String className = clazz.getInternalName();
        if (className.contains("/")) {
            sourcePath += className.substring(0, clazz.getInternalName().lastIndexOf("/") + 1);
        }

        return sourcePath;
    }

    /**
     * picks real source file name, will be used with JDWP ReferenceType(2).SourceFile(7) command
     */
    private String getJdwpSourceFile(Clazz clazz) {
        String sourceFile;
        // create source file name from class internal name to preserve full path to inner classes and
        // lambdas as it is required for dsymutils fix
        // but also look for "SourceFileTag" to pick up proper extension in case of kotlin and others
        SourceFileTag sourceFileTag = (SourceFileTag) clazz.getSootClass().getTag("SourceFileTag");
        if (sourceFileTag != null) {
            sourceFile = sourceFileTag.getSourceFile();
        } else {
            sourceFile = clazz.getInternalName();
            int sepIdx = sourceFile.lastIndexOf('/');
            if (sepIdx > 0)
                sourceFile = sourceFile.substring(sepIdx + 1);
            sepIdx = sourceFile.indexOf('$');
            if (sepIdx > 0)
                sourceFile = sourceFile.substring(0, sepIdx);

            // there is no name attached so guess it was compiled from java
            sourceFile += ".java";
        }

        return sourceFile;
    }

    /**
     * data bundle that contains debug information for class
     */
    private static class ClassDataBundle {
        DICompileUnit diCompileUnit;
        DIFile diFile;
        NamedMetadata flags;

        // information for debugger

        // basic type definitions (required for debugger purpose)
        // variable location information -- nobody cares (e.g. there is no debugger that relies on this data)
        // if future it shall be fixed
        private DIBasicType dummyJavaVariableType;

        // empty metadata -- to reuse where required
        private NamedMetadata<DIMetadataValueList> emptyMetadata;

        // basic subprogramType definition, will define just empty
        private DISubroutineType dummySubprogramType;

        // debug information for methods
        List<MethodDataBundle> methods;

        // method signatures before compilation starts
        Set<String> methodsBeforeCompile;

        DIBasicType getDummyJavaVariableType(ModuleBuilder mb) {
            if (dummyJavaVariableType == null) {
                dummyJavaVariableType = new DIBasicType(mb, "DummyType", 32, DwarfConst.TypeKind.DW_ATE_address);
            }
            return dummyJavaVariableType;
        }

        NamedMetadata<DIMetadataValueList> getEmptyMetadata(ModuleBuilder mb) {
            if (emptyMetadata == null) {
                emptyMetadata = new NamedMetadata<>(mb, new DIMetadataValueList());
            }
            return emptyMetadata;
        }

        DISubroutineType getDummySubprogramType(ModuleBuilder mb) {
            if (dummySubprogramType == null) {
                dummySubprogramType = new DISubroutineType(mb, getEmptyMetadata(mb));
            }
            return dummySubprogramType;
        }
    }

    /**
     * Data bundle that contains debug information about variables -- required for debugger only
     */
    private static class VariableDataBundle {
        final int codeIndex;
        final String name;
        final String dwarfName;
        final String typeSignature;
        final int startLine;
        final int finalLine;

        VariableDataBundle(int codeIndex, String name, String dwarfName, String typeSignature, int startLine, int finalLine) {
            this.codeIndex = codeIndex;
            this.name = name;
            this.dwarfName = dwarfName;
            this.typeSignature = typeSignature;
            this.startLine = startLine;
            this.finalLine = finalLine;
        }
    }

    /**
     * Data bundle that contains debug information about variables -- required for debugger only
     */
    private static class MethodDataBundle {
        final String signature;
        final int startLine;
        final int finalLine;
        final List<VariableDataBundle> variables;

        MethodDataBundle(String signature, int startLine, int finalLine, List<VariableDataBundle> variables) {
            this.signature = signature;
            this.startLine = startLine;
            this.finalLine = finalLine;
            this.variables = variables;
        }
    }

    /**
     * Used to minimize number for DILocation being created for same locations (e.g. with same line and cols)
     */
    private static class LocationCache {
        final ModuleBuilder mb;
        final DISubprogram scope;
        final Map<Long, DILocation> cache = new HashMap<>();

        LocationCache(ModuleBuilder mb, DISubprogram scope) {
            this.mb = mb;
            this.scope = scope;
        }

        DILocation get(int line, int col) {
            long key = (long) line  << 32;
            key += col;
            DILocation loc = cache.get(key);
            if (loc == null) {
                loc = new DILocation(mb, line, col, scope);
                cache.put(key, loc);
            }

            return loc;
        }
    }
}
